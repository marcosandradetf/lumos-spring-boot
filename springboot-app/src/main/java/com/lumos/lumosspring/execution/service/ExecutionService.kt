package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.execution.dto.ReserveDTO
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.pre_measurement.dto.PreMeasurementStreetDTO
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.team.Team
import com.lumos.lumosspring.team.TeamRepository
import org.springframework.stereotype.Service
@Service
class ExecutionService(
    private val materialStockRepository: MaterialStockRepository,
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository
) {

    fun reserve(reserveDTO: ReserveDTO) {
        var street = preMeasurementStreetRepository.findById(1L).orElseThrow()
        val team = teamRepository.findById(2L).orElseThrow()
        val truckDeposit = team.deposit ?: throw IllegalStateException("Equipe não possui depósito associado")

        // depositId instanciar deposito

        val items = street.items.orEmpty()
        val reservation = hashSetOf<MaterialReservation>()

        items.forEach { item ->
            val material = item.material ?: return@forEach

            if (reserveDTO.enjoyTuckDepositOfTeam) {
                val materialInTruck = truckDeposit.materialStocks
                    .filter { it.material != null }
                    .filter { it.material == material }
                    .firstOrNull { it.stockAvailable >= item.itemQuantity }

                if (materialInTruck != null) {
                    reservation.add(
                        MaterialReservation().apply {
                            description = "Reserva para execução da rua ${this.street?.street} na cidade de ${this.street?.city}"
                            materialStock = materialInTruck
                            setReservedQuantity(item.itemQuantity)
                            street = this.street
                        }
                    )
                } else {

                }
            }
        }


        // materialReservationRepository.saveAll(reservation)
    }
}
