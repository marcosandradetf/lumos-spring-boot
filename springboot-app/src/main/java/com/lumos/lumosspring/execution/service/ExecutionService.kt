package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.execution.dto.*
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.notification.service.NotificationService
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.ReservationManagement
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.stock.repository.ReservationManagementRepository
import com.lumos.lumosspring.team.repository.StockistRepository
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.ContractStatus
import com.lumos.lumosspring.util.DefaultResponse
import com.lumos.lumosspring.util.ReservationStatus
import com.lumos.lumosspring.util.Util
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository,
    private val notificationService: NotificationService,
    private val util: Util,
    private val stockistRepository: StockistRepository,
    private val userRepository: UserRepository,
    private val reservationManagementRepository: ReservationManagementRepository,
) {
    // delegar ao estoquista a função de GERENCIAR A RESERVA DE MATERIAIS
    fun delegate(delegateDTO: DelegateDTO): ResponseEntity<Any> {
        val stockist =
            userRepository.findByIdUser(UUID.fromString(delegateDTO.stockistId))
                .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DefaultResponse("Estoquista não encontrado"))

        val streets = preMeasurementStreetRepository.getAllByPreMeasurement_PreMeasurementIdAndStep(
            delegateDTO.preMeasurementId,
            delegateDTO.preMeasurementStep
        )

        if (streets.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Nenhuma rua foi Encontrada"))

        val existingManagement = reservationManagementRepository
            .existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
                delegateDTO.preMeasurementId,
                delegateDTO.preMeasurementStep
            )

        if (existingManagement) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(DefaultResponse("Já existe uma gestão de reserva para esse estoquista e essas ruas."))
        }

        val management = ReservationManagement()
        management.description = delegateDTO.description
        management.stockist = stockist

        reservationManagementRepository.save(management)

        val currentUserUUID = UUID.fromString(delegateDTO.currentUserUUID)
        for (delegateStreet in delegateDTO.street) {
            val team = teamRepository.findById(delegateStreet.teamId)
                .orElse(null) ?: return ResponseEntity.notFound().build()
            val assignBy = userRepository.findByIdUser(currentUserUUID)
                .orElse(null) ?: return ResponseEntity.notFound().build()
            val prioritized = delegateStreet.prioritized
            val comment = delegateStreet.comment

            streets.find { it.preMeasurementStreetId == delegateStreet.preMeasurementStreetId }
                ?.assignToStockistAndTeam(team, assignBy, util.dateTime, prioritized, comment, management)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(DefaultResponse("A rua ${delegateStreet.preMeasurementStreetId} enviada não foi encontrada"))

        }

        preMeasurementStreetRepository.saveAll(streets)

        return ResponseEntity.ok().build()
    }

    fun getPendingReservesForStockist(strUserUUID: String): ResponseEntity<Any> {
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val reserves = reservationManagementRepository.findAllByStatus(ReservationStatus.PENDING)
        val response = mutableListOf<ReserveDTOResponse>()

        for (reserve in reserves) {
            val description = reserve.description
            val stockistMatch = reserve.stockist.idUser == userUUID ||
                    reserve.stockist.stockist?.deposit?.stockists?.any { it.user.idUser == userUUID } == true

            if (stockistMatch) {
                val streets = reserve.streets
                    .sortedBy { it.prioritized == false }
                    .filter { it.streetStatus == ContractStatus.WAITING_STOCKIST }
                    .map { street ->
                        val items = street.items
                            .filter { item ->
                                val type = item.contractItem.referenceItem.type
                                !listOf("SERVIÇO", "PROJETO").contains(type)
                            }
                            .map { item ->
                                val ref = item.contractItem.referenceItem
                                ItemResponseDTO(
                                    itemId = item.preMeasurementStreetItemId,
                                    description = ref.nameForImport ?: description,
                                    quantity = item.measuredItemQuantity,
                                    type = ref.type,
                                    linking = ref.linking
                                )
                            }

                        ReserveStreetDTOResponse(
                            preMeasurementStreetId = street.preMeasurementStreetId,
                            streetName = street.street,
                            latitude = street.latitude,
                            longitude = street.longitude,
                            prioritized = street.prioritized,
                            comment = street.comment,
                            assignedBy = street.assignedBy.completedName,
                            teamName = street.team?.teamName ?: "",
                            truckDepositName = street.team?.deposit?.depositName ?: "",
                            items = items
                        )
                    }

                response.add(ReserveDTOResponse(description, streets))
            }
        }

        return ResponseEntity.ok(response)
    }

    fun getStockMaterialForLinking(linking: String, truckDepositName: String): ResponseEntity<Any> {
        val materials = materialStockRepository.findAllByLinking(linking.lowercase(), truckDepositName.lowercase())

        return ResponseEntity.ok(materials)
    }

    @Transactional
    fun reserveMaterialsForExecution(executionReserve: ReserveDTOCreate, strUserUUID: String): ResponseEntity<Any> {
        val preMeasurementStreet = preMeasurementStreetRepository.findById(executionReserve.preMeasurementStreetId)
            .orElse(null) ?: return ResponseEntity.status(404)
            .body(DefaultResponse("A rua ${executionReserve.preMeasurementStreetId} não foi encontrada"))

        if (preMeasurementStreet.streetStatus !== ContractStatus.WAITING_STOCKIST)
            return ResponseEntity.status(500)
                .body(DefaultResponse("Os itens dessa execução já foram todos reservados, inicie a próxima etapa."))

        val reservation = hashSetOf<MaterialReservation>()
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        for (item in executionReserve.items) {
            val materialStock = materialStockRepository.findById(item.materialId)
                .orElse(null) ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Material não encontrado")

            if (materialStock.stockAvailable < item.materialQuantity)
                return ResponseEntity.status(500)
                    .body("O material ${materialStock.material.materialName} não possuí estoque suficiente")

            val deposit = materialStock.deposit
            val stockistMatch = preMeasurementStreet.reservationManagement.stockist.idUser == userUUID ||
                    deposit.stockists.any { it.user.idUser == userUUID } ||
                    preMeasurementStreet.reservationManagement
                        .stockist.stockist?.deposit?.stockists?.any { it.user.idUser == userUUID } == true
            val teamMatch = materialStock.deposit.teams.isNotEmpty()

            reservation.add(
                MaterialReservation().apply {
                    this.description = ""
                    this.materialStock = materialStock
                    this.setReservedQuantity(item.materialQuantity)
                    this.street = preMeasurementStreet
                    if (teamMatch || stockistMatch) {
                        this.confirmReservation()
                        if (teamMatch) this.status = ReservationStatus.COLLECTED
                    }
                }
            )
        }

        if (reservation.all { it.status == ReservationStatus.APPROVED }) {
            preMeasurementStreet.streetStatus = ContractStatus.AVAILABLE_EXECUTION
        } else {
            preMeasurementStreet.streetStatus = ContractStatus.WAITING_RESERVE_CONFIRMATION
        }

        materialReservationRepository.saveAll(reservation)
        preMeasurementStreetRepository.save(preMeasurementStreet)

        if (preMeasurementStreet.reservationManagement.streets
                .none { it.streetStatus == ContractStatus.WAITING_STOCKIST }
        ) {
            preMeasurementStreet.reservationManagement.status = ReservationStatus.FINISHED
        }

        preMeasurementStreetRepository.save(preMeasurementStreet)

        return ResponseEntity.ok().build()
    }


//    Exemplo temos uma pre-medicao que contem diversas ruas e dentro de cada rua diversos itens,
//    o que eu preciso fazer é enviar essa referencia para o estoquista e ele irá verificar se cada equipe que ira executar o serviço
//    já tem estoque no caminhao, caso tenha ele irá reservar os materiais caso contrario ele ira reservar no proprio estoque
//    ou de terceiros (NESSE CASO ENVIAR UMA NOTIFICAÇÃO E REQUERER CONFIRMAÇÃO).

//     reserva
//fun reserve(reserveDTO: ReserveForStreetsDTO): ResponseEntity<Any> {
//        if (reserveDTO.streets.isEmpty()) return ResponseEntity.badRequest()
//            .body(ErrorResponse("Nenhum dado foi enviado"))
//
//        val firstDepositCity = depositRepository.findById(reserveDTO.firstDepositCityId).orElseThrow()
//        val secondDepositCity = depositRepository.findById(reserveDTO.secondDepositCityId).orElseThrow()
//        val preMeasurement = preMeasurementRepository.findById(reserveDTO.preMeasurementId).orElseThrow()
//        var quantityMaterials = 0
//
//
//        for (streetDTO in reserveDTO.streets) {
//            val pStreet = preMeasurement.streets.first { it.preMeasurementStreetId == streetDTO.preMeasurementStreetId }
//            val team = teamRepository.findById(streetDTO.teamId).orElseThrow()
//            val truckDeposit =
//                team.deposit ?: throw IllegalStateException("Equipe não possui almoxarifado associado")
//            pStreet.team = team
//
//            val items = pStreet.items.orEmpty()
//            val reservation = hashSetOf<MaterialReservation>()
//
//            for (item in items) {
//                val contractItem = item.contractItem ?: return ResponseEntity
//                    .badRequest().body(ErrorResponse("Material não encontrado"))
//
//                for (material in contractItem.referenceItem.materials) {
//                    val materialInTruck = truckDeposit.materialStocks
//                        .filter { it.material == material }
//                        .firstOrNull { it.stockAvailable >= item.measuredItemQuantity }
//                }
//
//
//                val description =
//                    "Reserva para execução da rua ${pStreet?.street} na cidade de ${pStreet?.city}"
//
//                if (materialInTruck != null) {
//                    quantityMaterials += 1
//                    reservation.add(
//                        MaterialReservation().apply {
//                            this.description = description
//                            this.truckDeposit = materialInTruck
//                            this.setReservedQuantity(item.measuredItemQuantity)
//                            this.street = pStreet
//                        }
//                    )
//                }
//
//                var materialInDeposit = firstDepositCity.materialStocks
//                    .filter { it.material == material }
//                    .firstOrNull { it.stockAvailable >= item.measuredItemQuantity }
//
//                if (materialInDeposit == null)
//                    return ResponseEntity.badRequest()
//                        .body(
//                            ErrorResponse(
//                                "Material '${material.materialName}' não possui quantidade suficiente no almoxarifado."
//                            )
//                        )
//
//                reservation.add(
//                    MaterialReservation().apply {
//                        this.description = description
//                        this.truckDeposit = materialInDeposit
//                        this.setReservedQuantity(item.measuredItemQuantity)
//                        this.preMeasurement = preMeasurement
//                        this.street = pStreet
//                    }
//                )
//
//                materialInDeposit = secondDepositCity.materialStocks
//                    .filter { it.material == material }
//                    .firstOrNull { it.stockAvailable >= item.measuredItemQuantity }
//
//                reservation.add(
//                    MaterialReservation().apply {
//                        this.description = description
//                        this.truckDeposit = materialInDeposit
//                        this.setReservedQuantity(item.measuredItemQuantity)
//                        this.preMeasurement = preMeasurement
//                        this.street = pStreet
//                        this.team = team
//                    }
//                )
//
//            }
//
//            materialReservationRepository.saveAll(reservation)
//            if (quantityMaterials > 0) {
//                notificationService.sendNotificationForTeam(
//                    title = "Confirme o estoque dos materiais",
//                    body = "$quantityMaterials materiais constam no sistema como em estoque no caminhao da equipe, verifique e confirme se possuí estoque",
//                    action = Routes.STOCK_CHECK,
//                    team = team.idTeam.toString(),
//                    time = util.dateTime,
//                    type = NotificationType.ALERT
//                )
//            }
//        }
//
//        return ResponseEntity.ok().body(ErrorResponse("Reserva de materiais salva com sucesso"))
//    }
//
//    fun getExecutions(userUUID: UUID): ResponseEntity<Any> {
//        val reservations = mutableListOf<MaterialReservation>()
//        val team = teamRepository.findByUserUUID(userUUID).orElse(null) ?: return ResponseEntity.notFound().build()
//        val streets = preMeasurementStreetRepository.findByTeam(team)
//
//
//        streets.flatMap { materialReservationRepository.findAllByStreet(it) }
//            .let { reservations.addAll(it) }
//
//
//        if (reservations.isEmpty()) {
//            return ResponseEntity.ok(emptyList<ReserveResponseDTO>())
//        }
//
//        val groupedReservations = reservations.groupBy { it.street }
//
//        val executionsResponse = groupedReservations.map { (street, reserves) ->
//            ExecutionDTO(
//                streetId = street.preMeasurementStreetId,
//                streetName = street.street,
//                teamId = street.team.idTeam,
//                teamName = street.team.teamName ?: "",
//                reserves = reserves.mapNotNull {
//                    val status = if (it.truckDeposit == null) "NOT_RESERVED" else "RESERVED"
//                    it.truckDeposit?.material?.let { material ->
//                        ReserveResponseDTO(
//                            reserveId = it.idMaterialReservation,
//                            materialId = material.idMaterial,
//                            materialName = material.materialName,
//                            materialQuantity = it.reservedQuantity,
//                            status = status
//                        )
//                    }
//                }
//            )
//        }
//
//
//        return ResponseEntity.ok(executionsResponse)
//    }
//
//    fun getStockRequests(userUUID: UUID): ResponseEntity<Any> {
//        val stockist =
//            stockistRepository.findByUserUUID(userUUID).orElse(null) ?: return ResponseEntity.notFound().build()
//
//        val reservations = materialReservationRepository.findAllByFirstDepositCityOrSecondDepositCity(
//            stockist.deposit.materialStocks
//        )
//
//        val reservationsResponse = reservations
//            .filter { it.truckDeposit == null }
//            .mapNotNull {
//                it.secondDepositCity?.material?.let { d ->
//                    ReserveResponseDTO(
//                        reserveId = it.idMaterialReservation,
//                        materialId = d.idMaterial,
//                        materialName = d.materialName,
//                        materialQuantity = it.reservedQuantity,
//                        status = "RESERVED"
//                    )
//                }
//            }
//
//        return ResponseEntity.ok(reservationsResponse)
//
//    }
//
//    fun reservationsReply(
//        approvals: List<ReplyReserveDTO>,
//        declinations: List<ReplyReserveDTO>,
//        streetId: Long,
//        userUUID: UUID
//    ): ResponseEntity<Any> {
//
//        val reservations = materialReservationRepository
//            .findAllByStreetPreMeasurementStreetId(streetId)
//            .orElse(emptyList())
//
//        val approvedIds = approvals.map { it.reserveId }.toSet()
//        val declinedIds = declinations.map { it.reserveId }.toSet()
//
//        val team = teamRepository.findByUserUUID(userUUID).orElse(null)
//        val stockist = stockistRepository.findByUserUUID(userUUID)
//        val firstDeposit = reservations.firstOrNull()?.firstDepositCity
//
//        val isTeam = team != null && reservations.any { it.team == team }
//
//        for (reserve in reservations) {
//            when {
//                stockist.isPresent -> {
//                    val isFirst = stockist.get().deposit == firstDeposit
//                    val location =
//                        if (isFirst) MaterialReservation.Location.FIRST else MaterialReservation.Location.SECOND
//
//                    when (reserve.idMaterialReservation) {
//                        in approvedIds -> reserve.confirmReservation(location)
//                        in declinedIds -> reserve.rejectReservation(location)
//                    }
//                }
//
//                isTeam -> {
//                    val location = MaterialReservation.Location.TRUCK
//                    when (reserve.idMaterialReservation) {
//                        in approvedIds -> reserve.confirmReservation(location)
//                        in declinedIds -> reserve.rejectReservation(location)
//                    }
//                }
//
//                else -> {
//                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(ErrorResponse("Usuário não tem permissão para responder a essa reserva"))
//                }
//            }
//        }
//
//        materialReservationRepository.saveAll(reservations)
//
//        return ResponseEntity.ok().build()
//    }

//    fun getStockAvailable(preMeasurementId: Long, teamId: Long): ResponseEntity<Any> {
//        val preMeasurement = preMeasurementRepository.findById(preMeasurementId).orElseThrow()
//        val team = teamRepository.findById(teamId).orElseThrow()
//        val streets = preMeasurement.streets
//        val localStockDTO = mutableListOf<LocalStockDTO>()
//
//        for (street in streets) {
//            val depositMaterials = mutableListOf<MaterialsInStockDTO>()
//            val truckMaterials = mutableListOf<MaterialsInStockDTO>()
//            val items = street.items
//            val materials = mutableListOf<Material>()
//            for (item in items) {
//                val material = item.material ?: continue
//                materials.add(material)
//            }
//
//            if (materials.isEmpty()) continue
//            val materialsStock = materialStockRepository.findAvailableFiltered(materials)
//            val materialsInTruck = materialsStock.filter { it.deposit.idDeposit == team.deposit.idDeposit }
//
//
//            for (material in materialsInTruck) {
//                truckMaterials.add(
//                    MaterialsInStockDTO(
//                        materialId = material.material.idMaterial,
//                        materialName = material.material.materialName,
//                        materialPower = material.material.materialPower,
//                        materialAmp = material.material.materialAmps,
//                        materialLength = material.material.materialLength,
//                        deposit = material.deposit.depositName,
//                        itemQuantity = street.items
//                            .first { it.material.idMaterial == material.material.idMaterial }.measuredItemQuantity,
//                        availableQuantity = material.stockAvailable
//                    )
//                )
//            }
//
//            localStockDTO.add(
//                LocalStockDTO(
//                    streetId = street.preMeasurementStreetId,
//                    materialsInStock = depositMaterials,
//                    materialsInTruck = truckMaterials,
//                )
//            )
//        }
//
//        return ResponseEntity.ok(localStockDTO)
//    }

//    fun getStockAvailableForStreet(streetId: Long, depositId: Long, truckDepositId: Long) {
//        val street = preMeasurementStreetRepository.findById(streetId).orElseThrow()
//        val depositMaterials = mutableListOf<MaterialsInStockDTO>()
//        val truckMaterials = mutableListOf<MaterialsInStockDTO>()
//        val items = street.items
//        val materials = mutableListOf<Material>()
//
//        for (item in items) {
//            val material = item.material ?: continue
//            materials.add(material)
//        }
//
//        val materialsStock = materialStockRepository.findAvailableFiltered(materials)
//        val materialsInDeposit = materialsStock.filter { it.deposit.idDeposit == depositId }
//        val materialsInTruck = materialsStock.filter { it.deposit.idDeposit == truckDepositId }
//
//        for (material in materialsInDeposit) {
//            depositMaterials.add(
//                MaterialsInStockDTO(
//                    materialId = material.material.idMaterial,
//                    materialName = material.material.materialName,
//                    materialPower = material.material.materialPower,
//                    materialAmp = material.material.materialAmps,
//                    materialLength = material.material.materialLength,
//                    deposit = material.deposit.depositName,
//                    itemQuantity = street.items
//                        .first { it.material.idMaterial == material.material.idMaterial }.measuredItemQuantity,
//                    availableQuantity = material.stockAvailable
//                )
//            )
//        }
//
//        for (material in materialsInTruck) {
//            truckMaterials.add(
//                MaterialsInStockDTO(
//                    materialId = material.material.idMaterial,
//                    materialName = material.material.materialName,
//                    materialPower = material.material.materialPower,
//                    materialAmp = material.material.materialAmps,
//                    materialLength = material.material.materialLength,
//                    deposit = material.deposit.depositName,
//                    itemQuantity = street.items
//                        .first { it.material.idMaterial == material.material.idMaterial }.measuredItemQuantity,
//                    availableQuantity = material.stockAvailable
//                )
//            )
//        }
//
//        LocalStockDTO(
//            streetId = street.preMeasurementStreetId,
//            materialsInStock = depositMaterials,
//            materialsInTruck = truckMaterials,
//        )
//
//    }
}
