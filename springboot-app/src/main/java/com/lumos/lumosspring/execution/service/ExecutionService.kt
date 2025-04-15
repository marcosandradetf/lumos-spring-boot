package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.execution.dto.LocalStockDTO
import com.lumos.lumosspring.execution.dto.MaterialsInStockDTO
import com.lumos.lumosspring.execution.dto.ReserveForStreetsDTO
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.notification.service.NotificationService
import com.lumos.lumosspring.notification.service.Routes
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementRepository
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.Material
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.team.TeamRepository
import com.lumos.lumosspring.user.Role
import com.lumos.lumosspring.util.ErrorResponse
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.Util
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val preMeasurementRepository: PreMeasurementRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository,
    private val notificationService: NotificationService,
    private val util: Util
) {

    fun reserve(reserveDTO: ReserveForStreetsDTO): ResponseEntity<Any> {
        if (reserveDTO.streets.isEmpty()) return ResponseEntity.badRequest()
            .body(ErrorResponse("Nenhum dado foi enviado"))

        val firstDepositCity = depositRepository.findById(reserveDTO.firstDepositCityId).orElseThrow()
        val secondDepositCity = depositRepository.findById(reserveDTO.secondDepositCityId).orElseThrow()
        val preMeasurement = preMeasurementRepository.findById(reserveDTO.preMeasurementId).orElseThrow()
        var quantityMaterials: Int = 0


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

    fun getReserve(preMeasurementId: Long) {
        var reservation = materialReservationRepository.finallBy
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
