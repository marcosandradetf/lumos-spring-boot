package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.execution.dto.*
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.notification.service.NotificationService
import com.lumos.lumosspring.notification.service.Routes
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementRepository
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.Material
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.team.repository.StockistRepository
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.util.ErrorResponse
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.Util
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val preMeasurementRepository: PreMeasurementRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository,
    private val notificationService: NotificationService,
    private val util: Util,
    private val stockistRepository: StockistRepository
) {

    fun reserve(reserveDTO: ReserveForStreetsDTO): ResponseEntity<Any> {
        if (reserveDTO.streets.isEmpty()) return ResponseEntity.badRequest()
            .body(ErrorResponse("Nenhum dado foi enviado"))

        val firstDepositCity = depositRepository.findById(reserveDTO.firstDepositCityId).orElseThrow()
        val secondDepositCity = depositRepository.findById(reserveDTO.secondDepositCityId).orElseThrow()
        val preMeasurement = preMeasurementRepository.findById(reserveDTO.preMeasurementId).orElseThrow()
        var quantityMaterials = 0


        for (streetDTO in reserveDTO.streets) {
            val pStreet = preMeasurement.streets.first { it.preMeasurementStreetId == streetDTO.preMeasurementStreetId }
            val team = teamRepository.findById(streetDTO.teamId).orElseThrow()
            val truckDeposit =
                team.deposit ?: throw IllegalStateException("Equipe não possui almoxarifado associado")
            pStreet.team = team

            val items = pStreet.items.orEmpty()
            val reservation = hashSetOf<MaterialReservation>()

            for (item in items) {
                val material = item.material ?: return ResponseEntity
                    .badRequest().body(ErrorResponse("Material não encontrado"))

                val materialInTruck = truckDeposit.materialStocks
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.measuredItemQuantity }

                val description =
                    "Reserva para execução da rua ${pStreet?.street} na cidade de ${pStreet?.city}"

                if (materialInTruck != null) {
                    quantityMaterials += 1
                    reservation.add(
                        MaterialReservation().apply {
                            this.description = description
                            this.truckDeposit = materialInTruck
                            this.setReservedQuantity(item.measuredItemQuantity)
                            this.street = pStreet
                        }
                    )
                }

                var materialInDeposit = firstDepositCity.materialStocks
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.measuredItemQuantity }

                if (materialInDeposit == null)
                    return ResponseEntity.badRequest()
                        .body(
                            ErrorResponse(
                                "Material '${material.materialName}' não possui quantidade suficiente no almoxarifado."
                            )
                        )

                reservation.add(
                    MaterialReservation().apply {
                        this.description = description
                        this.truckDeposit = materialInDeposit
                        this.setReservedQuantity(item.measuredItemQuantity)
                        this.preMeasurement = preMeasurement
                        this.street = pStreet
                    }
                )

                materialInDeposit = secondDepositCity.materialStocks
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.measuredItemQuantity }

                reservation.add(
                    MaterialReservation().apply {
                        this.description = description
                        this.truckDeposit = materialInDeposit
                        this.setReservedQuantity(item.measuredItemQuantity)
                        this.preMeasurement = preMeasurement
                        this.street = pStreet
                        this.team = team
                    }
                )

            }

            materialReservationRepository.saveAll(reservation)
            if (quantityMaterials > 0) {
                notificationService.sendNotificationForTeam(
                    title = "Confirme o estoque dos materiais",
                    body = "$quantityMaterials materiais constam no sistema como em estoque no caminhao da equipe, verifique e confirme se possuí estoque",
                    action = Routes.STOCK_CHECK,
                    team = team.idTeam.toString(),
                    time = util.dateTime,
                    type = NotificationType.ALERT
                )
            }
        }

        return ResponseEntity.ok().body(ErrorResponse("Reserva de materiais salva com sucesso"))
    }

    fun getExecutions(userUUID: UUID): ResponseEntity<Any> {
        val reservations = mutableListOf<MaterialReservation>()
        val team = teamRepository.findByUserUUID(userUUID).orElse(null) ?: return ResponseEntity.notFound().build()
        val streets = preMeasurementStreetRepository.findByTeam(team)


        streets.flatMap { materialReservationRepository.findAllByStreet(it) }
            .let { reservations.addAll(it) }


        if (reservations.isEmpty()) {
            return ResponseEntity.ok(emptyList<ReserveResponseDTO>())
        }

        val groupedReservations = reservations.groupBy { it.street }

        val executionsResponse = groupedReservations.map { (street, reserves) ->
            ExecutionDTO(
                streetId = street.preMeasurementStreetId,
                streetName = street.street,
                teamId = street.team.idTeam,
                teamName = street.team.teamName ?: "",
                reserves = reserves.mapNotNull {
                    val status = if (it.truckDeposit == null) "NOT_RESERVED" else "RESERVED"
                    it.truckDeposit?.material?.let { material ->
                        ReserveResponseDTO(
                            reserveId = it.idMaterialReservation,
                            materialId = material.idMaterial,
                            materialName = material.materialName,
                            materialQuantity = it.reservedQuantity,
                            status = status
                        )
                    }
                }
            )
        }


        return ResponseEntity.ok(executionsResponse)
    }

    fun getStockRequests(userUUID: UUID): ResponseEntity<Any> {
        val stockist =
            stockistRepository.findByUserUUID(userUUID).orElse(null) ?: return ResponseEntity.notFound().build()

        val reservations = materialReservationRepository.findAllByFirstDepositCityOrSecondDepositCity(
            stockist.deposit.materialStocks
        )

        val reservationsResponse = reservations
            .filter { it.truckDeposit == null }
            .mapNotNull {
                it.secondDepositCity?.material?.let { d ->
                    ReserveResponseDTO(
                        reserveId = it.idMaterialReservation,
                        materialId = d.idMaterial,
                        materialName = d.materialName,
                        materialQuantity = it.reservedQuantity,
                        status = "RESERVED"
                    )
                }
            }

        return ResponseEntity.ok(reservationsResponse)

    }

    fun reservationsReply(
        approvals: List<ReplyReserveDTO>,
        declinations: List<ReplyReserveDTO>,
        streetId: Long,
        userUUID: UUID
    ): ResponseEntity<Any> {

        val reservations = materialReservationRepository
            .findAllByStreetPreMeasurementStreetId(streetId)
            .orElse(emptyList())

        val approvedIds = approvals.map { it.reserveId }.toSet()
        val declinedIds = declinations.map { it.reserveId }.toSet()

        val team = teamRepository.findByUserUUID(userUUID).orElse(null)
        val stockist = stockistRepository.findByUserUUID(userUUID)
        val firstDeposit = reservations.firstOrNull()?.firstDepositCity

        val isTeam = team != null && reservations.any { it.team == team }

        for (reserve in reservations) {
            when {
                stockist.isPresent -> {
                    val isFirst = stockist.get().deposit == firstDeposit
                    val location =
                        if (isFirst) MaterialReservation.Location.FIRST else MaterialReservation.Location.SECOND

                    when (reserve.idMaterialReservation) {
                        in approvedIds -> reserve.confirmReservation(location)
                        in declinedIds -> reserve.rejectReservation(location)
                    }
                }

                isTeam -> {
                    val location = MaterialReservation.Location.TRUCK
                    when (reserve.idMaterialReservation) {
                        in approvedIds -> reserve.confirmReservation(location)
                        in declinedIds -> reserve.rejectReservation(location)
                    }
                }

                else -> {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ErrorResponse("Usuário não tem permissão para responder a essa reserva"))
                }
            }
        }

        materialReservationRepository.saveAll(reservations)

        return ResponseEntity.ok().build()
    }

    fun getStockAvailable(preMeasurementId: Long, teamId: Long): ResponseEntity<Any> {
        val preMeasurement = preMeasurementRepository.findById(preMeasurementId).orElseThrow()
        val team = teamRepository.findById(teamId).orElseThrow()
        val streets = preMeasurement.streets
        val localStockDTO = mutableListOf<LocalStockDTO>()

        for (street in streets) {
            val depositMaterials = mutableListOf<MaterialsInStockDTO>()
            val truckMaterials = mutableListOf<MaterialsInStockDTO>()
            val items = street.items
            val materials = mutableListOf<Material>()
            for (item in items) {
                val material = item.material ?: continue
                materials.add(material)
            }

            if (materials.isEmpty()) continue
            val materialsStock = materialStockRepository.findAvailableFiltered(materials)
            val materialsInTruck = materialsStock.filter { it.deposit.idDeposit == team.deposit.idDeposit }


            for (material in materialsInTruck) {
                truckMaterials.add(
                    MaterialsInStockDTO(
                        materialId = material.material.idMaterial,
                        materialName = material.material.materialName,
                        materialPower = material.material.materialPower,
                        materialAmp = material.material.materialAmps,
                        materialLength = material.material.materialLength,
                        deposit = material.deposit.depositName,
                        itemQuantity = street.items
                            .first { it.material.idMaterial == material.material.idMaterial }.measuredItemQuantity,
                        availableQuantity = material.stockAvailable
                    )
                )
            }

            localStockDTO.add(
                LocalStockDTO(
                    streetId = street.preMeasurementStreetId,
                    materialsInStock = depositMaterials,
                    materialsInTruck = truckMaterials,
                )
            )
        }

        return ResponseEntity.ok(localStockDTO)
    }

    fun getStockAvailableForStreet(streetId: Long, depositId: Long, truckDepositId: Long) {
        val street = preMeasurementStreetRepository.findById(streetId).orElseThrow()
        val depositMaterials = mutableListOf<MaterialsInStockDTO>()
        val truckMaterials = mutableListOf<MaterialsInStockDTO>()
        val items = street.items
        val materials = mutableListOf<Material>()

        for (item in items) {
            val material = item.material ?: continue
            materials.add(material)
        }

        val materialsStock = materialStockRepository.findAvailableFiltered(materials)
        val materialsInDeposit = materialsStock.filter { it.deposit.idDeposit == depositId }
        val materialsInTruck = materialsStock.filter { it.deposit.idDeposit == truckDepositId }

        for (material in materialsInDeposit) {
            depositMaterials.add(
                MaterialsInStockDTO(
                    materialId = material.material.idMaterial,
                    materialName = material.material.materialName,
                    materialPower = material.material.materialPower,
                    materialAmp = material.material.materialAmps,
                    materialLength = material.material.materialLength,
                    deposit = material.deposit.depositName,
                    itemQuantity = street.items
                        .first { it.material.idMaterial == material.material.idMaterial }.measuredItemQuantity,
                    availableQuantity = material.stockAvailable
                )
            )
        }

        for (material in materialsInTruck) {
            truckMaterials.add(
                MaterialsInStockDTO(
                    materialId = material.material.idMaterial,
                    materialName = material.material.materialName,
                    materialPower = material.material.materialPower,
                    materialAmp = material.material.materialAmps,
                    materialLength = material.material.materialLength,
                    deposit = material.deposit.depositName,
                    itemQuantity = street.items
                        .first { it.material.idMaterial == material.material.idMaterial }.measuredItemQuantity,
                    availableQuantity = material.stockAvailable
                )
            )
        }

        LocalStockDTO(
            streetId = street.preMeasurementStreetId,
            materialsInStock = depositMaterials,
            materialsInTruck = truckMaterials,
        )

    }
}
