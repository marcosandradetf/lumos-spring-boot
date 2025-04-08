package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.execution.dto.MaterialsInStockDTO
import com.lumos.lumosspring.execution.dto.ReserveDTO
import com.lumos.lumosspring.execution.dto.LocalStockDTO
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementRepository
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.Material
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.team.TeamRepository
import com.lumos.lumosspring.util.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val preMeasurementRepository: PreMeasurementRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository
) {

    fun reserve(reserveDTO: List<ReserveDTO>): ResponseEntity<Any> {
        if (reserveDTO.isEmpty()) return ResponseEntity.badRequest()
            .body(ErrorResponse("Nenhum dado foi enviado"))

        for (reserve in reserveDTO) {
            val street = preMeasurementStreetRepository.findById(reserve.preMeasurementStreetId).orElseThrow()
            val team = teamRepository.findById(reserve.teamId).orElseThrow()
            val truckDeposit = team.deposit ?: throw IllegalStateException("Equipe não possui depósito associado")
            val deposit = depositRepository.findById(reserve.depositId).orElseThrow()

            val items = street.items.orEmpty()
            val reservation = hashSetOf<MaterialReservation>()

            for (item in items) {
                val material = item.material ?: return ResponseEntity
                    .badRequest().body(ErrorResponse("Material não encontrado"))

                val materialInTruck = truckDeposit.materialStocks
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.itemQuantity }

                val description =
                    "Reserva para execução da rua ${street?.street} na cidade de ${street?.city}"

                if (materialInTruck != null) {
                    reservation.add(
                        MaterialReservation().apply {
                            this.description = description
                            this.materialStock = materialInTruck
                            this.setReservedQuantity(item.itemQuantity)
                            this.street = street
                        }
                    )
                } else {
                    val materialInDeposit = deposit.materialStocks
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
                            this.materialStock = materialInDeposit
                            this.setReservedQuantity(item.itemQuantity)
                            this.street = street
                        }
                    )

                }
            }

            materialReservationRepository.saveAll(reservation)
        }

        return ResponseEntity.ok().body(ErrorResponse("Reserva de materiais salva com sucesso"))
    }

    fun getStockAvailable(preMeasurementId: Long, depositId: Long, truckDepositId: Long) {
        val preMeasurement = preMeasurementRepository.findById(preMeasurementId).orElseThrow()
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
            val materialsInDeposit = materialsStock.filter { it.deposit.idDeposit == depositId }
            val materialsInTruck = materialsStock.filter { it.deposit.idDeposit == truckDepositId }

            for (material in materialsInDeposit) {
                depositMaterials.add(
                    MaterialsInStockDTO(
                        materialId = material.material.idMaterial,
                        materialName = material.material.materialName,
                        deposit = material.deposit.depositName,
                        availableQuantity = material.stockAvailable
                    )
                )
            }

            for (material in materialsInTruck) {
                truckMaterials.add(
                    MaterialsInStockDTO(
                        materialId = material.material.idMaterial,
                        materialName = material.material.materialName,
                        deposit = material.deposit.depositName,
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
                    deposit = material.deposit.depositName,
                    availableQuantity = material.stockAvailable
                )
            )
        }

        for (material in materialsInTruck) {
            truckMaterials.add(
                MaterialsInStockDTO(
                    materialId = material.material.idMaterial,
                    materialName = material.material.materialName,
                    deposit = material.deposit.depositName,
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
