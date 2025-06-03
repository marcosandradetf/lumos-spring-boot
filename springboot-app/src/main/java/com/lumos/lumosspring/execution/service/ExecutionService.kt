package com.lumos.lumosspring.execution.service

import com.google.protobuf.LazyStringArrayList.emptyList
import com.lumos.lumosspring.execution.dto.*
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.fileserver.service.MinioService
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.ReservationManagement
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.stock.repository.ReservationManagementRepository
import com.lumos.lumosspring.team.repository.StockistRepository
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.*
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2

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
    private val minioService: MinioService,
    private val jdbcTemplate: JdbcTemplate,
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
                            teamId = street.team?.idTeam ?: 0L,
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

    fun getStockMaterialForLinking(linking: String, type: String, truckDepositName: String): ResponseEntity<Any> {
        var materials: List<MaterialInStockDTO>

        if (type != "NULL" && linking != "NULL") {
            materials = materialStockRepository.findAllByLinkingAndType(
                linking.lowercase(),
                type.lowercase(),
                truckDepositName.lowercase()
            )
        } else {
            materials = materialStockRepository.findAllByType(type.lowercase(), truckDepositName.lowercase())
        }

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

        val reservation = mutableListOf<MaterialReservation>()
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        for (item in executionReserve.items) {
            for (materialReserve in item.materials) {
                val materialStock = materialStockRepository.findById(materialReserve.materialId)
                    .orElse(null) ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(DefaultResponse("Material não encontrado"))

                if (materialStock.stockAvailable < materialReserve.materialQuantity)
                    return ResponseEntity.status(500)
                        .body(DefaultResponse("O material ${materialStock.material.materialName} não possuí estoque suficiente"))

                val deposit = materialStock.deposit
                val stockistMatch = preMeasurementStreet.reservationManagement.stockist.idUser == userUUID ||
                        deposit.stockists.any { it.user.idUser == userUUID } ||
                        preMeasurementStreet.reservationManagement
                            .stockist.stockist?.deposit?.stockists?.any { it.user.idUser == userUUID } == true

                val teamMatch = materialStock.deposit.teams.isNotEmpty()
                val contractItemId = util.getDescription(
                    field = "contract_item_id",
                    table = "tb_pre_measurements_streets_items",
                    where = "pre_measurement_street_item_id",
                    equal = item.itemId.toString(),
                    type = Long::class.java,
                )
                if (contractItemId == null)
                    return ResponseEntity.status(500)
                        .body(DefaultResponse("Contrato do item ${item.itemId} enviado não foi encontrado"))

                reservation.add(
                    MaterialReservation().apply {
                        this.description = ""
                        this.materialStock = materialStock
                        this.reservedQuantity = materialReserve.materialQuantity
                        this.street = preMeasurementStreet
                        this.contractItemId = contractItemId
                        if (teamMatch || stockistMatch) {
                            this.confirmReservation()
                            if (teamMatch) this.status = ReservationStatus.COLLECTED
                        }
                    }
                )
            }
        }

        materialReservationRepository.saveAll(reservation)

        var responseMessage = ""
        if (reservation.all { it.status == ReservationStatus.COLLECTED }) {
            preMeasurementStreet.streetStatus = ContractStatus.AVAILABLE_EXECUTION
            responseMessage =
                "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. A equipe pode iniciar a execução."

        } else if (!reservation.any { it.status == ReservationStatus.PENDING }) {
            preMeasurementStreet.streetStatus = ContractStatus.AVAILABLE_EXECUTION
            responseMessage =
                "Como nenhum material foi reservado em almoxarifado de terceiros. Não será necessário aprovação. " +
                        "Mas os materiais estão pendentes de coleta pela equipe."

        } else if (reservation.any { it.status == ReservationStatus.PENDING }) {
            preMeasurementStreet.streetStatus = ContractStatus.WAITING_RESERVE_CONFIRMATION

            responseMessage =
                "Como alguns itens foram reservadas em almoxarifados de terceiros. Será necessária aprovação. " +
                        "Após isso estes materiais estarão disponíveis para coleta."
        }

        notify(reservation, preMeasurementStreet.preMeasurementStreetId.toString())

        preMeasurementStreetRepository.save(preMeasurementStreet)

        if (preMeasurementStreet.reservationManagement.streets
                .none { it.streetStatus == ContractStatus.WAITING_STOCKIST }
        ) {
            preMeasurementStreet.reservationManagement.status = ReservationStatus.FINISHED
        }

        preMeasurementStreetRepository.save(preMeasurementStreet)

        return ResponseEntity.ok().body(DefaultResponse(responseMessage))
    }

    private fun notify(reservation: List<MaterialReservation>, preMeasurementStreetId: String) {
        var bodyMessage: String
        val companyPhone = util.getDescription(
            field = "company_phone",
            table = "tb_companies",
            type = String::class.java,
        )

        val groupedByDepositPending = reservation
            .filter { it.status == ReservationStatus.PENDING }
            .groupBy { it.materialStock?.deposit }

        val groupedByDepositApproved = reservation
            .filter { it.status == ReservationStatus.APPROVED }
            .groupBy { it.materialStock?.deposit }

        // Itera sobre os grupos e envia notificação para equipe por depósito
        groupedByDepositPending.forEach { (deposit, reserve) ->
            val teamCodes = deposit?.teams?.map { it.teamCode } ?: emptyList()

            bodyMessage = """
                Por favor, aceite ou negar as reservas com urgência!
                """.trimIndent()

            teamCodes.forEach { it ->
                notificationService.sendNotificationForTeam(
                    team = it,
                    title = "Existem ${reserve.size} materiais pendentes de aprovação no seu almoxarifado (${deposit?.depositName ?: ""})",
                    body = bodyMessage,
                    action = "REPLY_RESERVE",
                    time = util.dateTime,
                    type = NotificationType.ALERT,
                    persistCode = preMeasurementStreetId
                )
            }

        }

        // Itera sobre os grupos e envia notificação para equipe por depósito
        groupedByDepositApproved.forEach { (deposit, reserve) ->
            val teamCode = reserve.first().team?.teamCode ?: ""
            val quantity = reserve.size

            val depositName = deposit?.depositName ?: "Desconhecido"
            val address = deposit?.depositAddress ?: "Endereço não informado"
            val phone = deposit?.depositPhone ?: companyPhone ?: "Telefone não Informado"
            val responsible = deposit?.stockists
                ?.firstOrNull()
                ?.user
                ?.completedName
                ?: "Responsável não informado"

            val bodyMessage = """
                    Local: $depositName"
                    Endereço: $address"
                    Telefone: $phone"
                    Responsável: $responsible"
                """.trimIndent()

            notificationService.sendNotificationForTeam(
                team = teamCode,
                title = "Existem $quantity materiais pendentes de coleta no almoxarifado ${deposit?.depositName ?: ""}",
                body = bodyMessage,
                action = "",
                time = util.dateTime,
                type = NotificationType.ALERT,
                persistCode = preMeasurementStreetId
            )

        }

    }

    @Transactional
    fun uploadData(photo: MultipartFile, executionDTO: SendExecutionDto?): ResponseEntity<Any> {
        if (executionDTO == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        val fileUri = minioService.uploadFile(photo, "scl-construtora")

        val execution = preMeasurementStreetRepository.findById(executionDTO.streetId)
            .orElseThrow { IllegalArgumentException("Street com ID ${executionDTO.streetId} não encontrada") }

        if(execution.streetStatus != ContractStatus.AVAILABLE_EXECUTION) {
            return ResponseEntity.badRequest().body("Execução já enviada")
        }

        execution.photoUri = fileUri
        execution.streetStatus = ContractStatus.FINISHED
        preMeasurementStreetRepository.save(execution)

        for (r in executionDTO.reserves) {
            val reserve = materialReservationRepository.findById(r.reserveId)
                .orElseThrow { IllegalArgumentException("Reserva com ID ${r.reserveId} não encontrada") }

            reserve.setQuantityCompleted(r.quantityExecuted)
            materialReservationRepository.save(reserve)

            val contractItemId = reserve.contractItemId
            val sql = "UPDATE tb_contracts_items set quantity_executed = ? where contract_item_id = ?"
            jdbcTemplate.update(sql, r.quantityExecuted, contractItemId)
        }


        return ResponseEntity.ok().build()
    }

    fun getExecutions(strUUID: String?): ResponseEntity<MutableList<ExecutionDTO>> {
        val uuid = strUUID ?: return ResponseEntity.badRequest().body(arrayListOf())

        val user = userRepository.findById(UUID.fromString(uuid))
            .orElseThrow { IllegalArgumentException("Equipe não encontrada") }

        val teamsId = when {
            user.electricians.isNotEmpty() -> user.electricians.map { it.idTeam }
            user.drivers.isNotEmpty() -> user.drivers.map { it.idTeam }
            else -> return ResponseEntity.badRequest().body(arrayListOf())
        }

        val streets = preMeasurementStreetRepository.findByTeam_IdTeam(teamsId)

        val reservationsByStreet = materialReservationRepository
            .findAllByStreetInStreetId(streets.map { it.streetId })
            .groupBy { it.street.preMeasurementStreetId }

        val reservesByStreet = reservationsByStreet.mapValues { (_, reservations) ->
            reservations.map { r ->
                val materialStock = r.materialStock
                val deposit = materialStock?.deposit
                val stockist = deposit?.stockists?.firstOrNull()?.user

                Reserve(
                    reserveId = r.idMaterialReservation,
                    materialName = materialStock?.material?.nameForImport ?: "",
                    materialQuantity = r.reservedQuantity,
                    reserveStatus = r.status,
                    streetId = r.street.preMeasurementStreetId,
                    depositId = deposit?.idDeposit ?: 0L,
                    depositName = deposit?.depositName ?: "Desconhecido",
                    depositAddress = deposit?.depositAddress ?: "Desconhecido",
                    stockistName = stockist?.completedName ?: "Desconhecido",
                    phoneNumber = stockist?.phoneNumber ?: "Desconhecido",
                    requestUnit = materialStock?.requestUnit ?: "UN"
                )
            }
        }

        val executions = streets.map { street ->
            ExecutionDTO(
                streetId = street.streetId,
                streetName = street.streetName,
                streetNumber = street.streetNumber,
                streetHood = street.streetHood,
                city = street.city,
                state = street.state,
                teamName = street.teamName,
                priority = street.priority,
                type = street.type,
                itemsQuantity = street.itemsQuantity,
                creationDate = street.creationDate.toString(),
                latitude = street.latitude,
                longitude = street.longitude,
                reserves = reservesByStreet[street.streetId] ?: listOf()
            )
        }.toMutableList()

        return ResponseEntity.ok().body(executions)

    }


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

}
