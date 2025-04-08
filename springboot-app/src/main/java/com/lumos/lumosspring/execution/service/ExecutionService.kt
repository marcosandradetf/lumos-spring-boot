package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.execution.dto.ReserveDTO
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.pre_measurement.dto.PreMeasurementStreetDTO
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.team.Team
import com.lumos.lumosspring.team.TeamRepository
import com.lumos.lumosspring.util.ErrorResponse
import io.grpc.Status
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository
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

            items.forEach { item ->
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
}
