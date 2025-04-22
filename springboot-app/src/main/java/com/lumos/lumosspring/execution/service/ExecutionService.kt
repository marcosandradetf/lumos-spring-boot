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
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.ErrorResponse
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.Util
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.UUID

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
                    .firstOrNull { it.stockAvailable >= item.itemQuantity }

                val description =
                    "Reserva para execução da rua ${pStreet?.street} na cidade de ${pStreet?.city}"

                if (materialInTruck != null) {
                    quantityMaterials += 1
                    reservation.add(
                        MaterialReservation().apply {
                            this.description = description
                            this.truckDeposit = materialInTruck
                            this.setReservedQuantity(item.itemQuantity)
                            this.street = pStreet
                        }
                    )
                }

                var materialInDeposit = firstDepositCity.materialStocks
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.itemQuantity }

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
                        this.setReservedQuantity(item.itemQuantity)
                        this.preMeasurement = preMeasurement
                        this.street = pStreet
                    }
                )

                materialInDeposit = secondDepositCity.materialStocks
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.itemQuantity }

                reservation.add(
                    MaterialReservation().apply {
                        this.description = description
                        this.truckDeposit = materialInDeposit
                        this.setReservedQuantity(item.itemQuantity)
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

    fun getReservations(streetId: Long, userUUID: UUID): ResponseEntity<Any> {
        val reservations =
            materialReservationRepository.findAllByStreetPreMeasurementStreetId(streetId).orElse(emptyList())
        val teams = teamRepository.findByUserUUID(userUUID)
        var isTeam = false
        val reservationsResponse = mutableListOf<ReserveResponseDTO>()
        val firstDeposit = reservations.first().firstDepositCity

        if (!teams.isEmpty) {
            for (team in teams.get()) {
                if (team == reservations[0].team) {
                    isTeam = true
                }
            }
        }

        when (isTeam) {
            true -> {
                reservations.map {
                    if (it.truckDeposit !== null) {
                        it.truckDeposit?.material?.let { d ->
                            reservationsResponse.add(
                                ReserveResponseDTO(
                                    materialName = d.materialName,
                                    materialQuantity = it.reservedQuantity,
                                    streetName = it.street.street
                                )
                            )
                        }
                    }
                }
            }

            false -> {
                val stockist = stockistRepository.findByUserUUID(userUUID)
                if (!stockist.isEmpty) {
                    when (firstDeposit == stockist.get().deposit) {
                        true -> {
                            reservations.map {
                                if (it.truckDeposit == null) {
                                    it.secondDepositCity?.material?.let { d ->
                                        reservationsResponse.add(
                                            ReserveResponseDTO(
                                                materialName = d.materialName,
                                                materialQuantity = it.reservedQuantity,
                                                streetName = it.street.street
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        false -> {
                            reservations.map {
                                if (it.truckDeposit == null && firstDeposit == null) {
                                    it.secondDepositCity?.material?.let { d ->
                                        reservationsResponse.add(
                                            ReserveResponseDTO(
                                                materialName = d.materialName,
                                                materialQuantity = it.reservedQuantity,
                                                streetName = it.street.street
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
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

        val teams = teamRepository.findByUserUUID(userUUID)
        val stockist = stockistRepository.findByUserUUID(userUUID)
        val firstDeposit = reservations.firstOrNull()?.firstDepositCity

        val isTeam = teams.isPresent && teams.get().any { it == reservations.first().team }

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
                            .first { it.material.idMaterial == material.material.idMaterial }.itemQuantity,
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
                        .first { it.material.idMaterial == material.material.idMaterial }.itemQuantity,
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
                        .first { it.material.idMaterial == material.material.idMaterial }.itemQuantity,
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
