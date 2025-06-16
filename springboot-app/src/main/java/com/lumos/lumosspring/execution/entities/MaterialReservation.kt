package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.stock.entities.MaterialStock
import com.lumos.lumosspring.stock.entities.StockMovement.Status
import com.lumos.lumosspring.team.entities.Team
import com.lumos.lumosspring.util.ReservationStatus
import jakarta.persistence.*

@Entity
@Table(name = "tb_material_reservation")
class MaterialReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material_reservation")
    var idMaterialReservation: Long = 0

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @ManyToOne
    @JoinColumn(name = "material_stock_id")
    var materialStock: MaterialStock? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "pre_measurement_street_id")
    var street: PreMeasurementStreet? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "direct_execution_id")
    var directExecution: DirectExecution? = null

    @JoinColumn(name = "contract_item_id")
    var contractItemId: Long = 0

    @Column(nullable = false)
    var reservedQuantity: Double = 0.0

    @Column(nullable = false)
    var quantityCompleted: Double = 0.0
        private set

    var status: String = ReservationStatus.PENDING

    @ManyToOne
    @JoinColumn(name = "team_id")
    var team: Team? = null


    fun confirmReservation() {
        materialStock?.removeStockAvailable(reservedQuantity)
        status = ReservationStatus.APPROVED
    }

    fun rejectReservation() {
        materialStock?.let {
            it.addStockAvailable(reservedQuantity)
            materialStock = null
            status = ReservationStatus.REJECTED
        }
    }

    fun setQuantityCompleted(quantityCompleted: Double) {
        this.quantityCompleted = quantityCompleted

        materialStock?.let {
            it.removeStockQuantity(quantityCompleted)
            it.addStockAvailable(reservedQuantity)
            it.removeStockAvailable(quantityCompleted)
        }

        status = ReservationStatus.FINISHED
    }

}



