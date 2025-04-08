package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.stock.entities.MaterialStock
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

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "material_stock_id")
    var materialStock: MaterialStock? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    var street: PreMeasurementStreet? = null

    @Column(nullable = false)
    var reservedQuantity: Double = 0.0
        private set

    @Column(nullable = false)
    var quantityCompleted: Double = 0.0
        private set

    var status: String = "PENDING"

    fun setReservedQuantity(reservedQuantity: Double) {
        this.reservedQuantity = reservedQuantity
        materialStock!!.removeStockAvailable(reservedQuantity)
    }

    fun setQuantityCompleted(quantityCompleted: Int) {
        this.quantityCompleted = quantityCompleted.toDouble()
        materialStock!!.removeStockQuantity(quantityCompleted)
    }

    private fun removeStockAvailable() {
        val qtStockAvailable = materialStock!!.stockAvailable
        if (qtStockAvailable > 0) {
            materialStock!!.removeStockAvailable(this.reservedQuantity)
        }
    }


}


