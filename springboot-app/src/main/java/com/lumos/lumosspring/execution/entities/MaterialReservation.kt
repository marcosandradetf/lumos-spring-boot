package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.stock.entities.MaterialStock
import com.lumos.lumosspring.stock.entities.StockMovement.Status
import com.lumos.lumosspring.team.entities.Team
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
    @JoinColumn(name = "material_truck_stock_id")
    var truckDeposit: MaterialStock? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "material_first_deposit_stock_id")
    var firstDepositCity: MaterialStock? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "material_second_deposit_stock_id")
    var secondDepositCity: MaterialStock? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "pre_measurement_id")
    var preMeasurement: PreMeasurement? = null

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "pre_measurement_street_id")
    var street: PreMeasurementStreet = PreMeasurementStreet()

    @Column(nullable = false)
    var reservedQuantity: Double = 0.0
        private set

    @Column(nullable = false)
    var quantityCompleted: Double = 0.0
        private set

    var status: String = "PENDING"

    @ManyToOne
    @JoinColumn(name = "team_id")
    var team: Team? = null

    fun setReservedQuantity(reservedQuantity: Double) {
        this.reservedQuantity = reservedQuantity
    }

    fun removeStockAvailable() {
        if (truckDeposit != null && firstDepositCity != null && secondDepositCity != null) {
            truckDeposit!!.removeStockAvailable(reservedQuantity)
            firstDepositCity!!.removeStockAvailable(reservedQuantity)
            secondDepositCity!!.removeStockAvailable(reservedQuantity)
        }
    }

    fun confirmReservation(location: Location) {
        when (location) {
            Location.TRUCK -> {
                firstDepositCity!!.addStockAvailable(reservedQuantity)
                secondDepositCity!!.addStockAvailable(reservedQuantity)
                firstDepositCity = null
                secondDepositCity = null
            }

            Location.FIRST -> {
                truckDeposit!!.addStockAvailable(reservedQuantity)
                secondDepositCity!!.addStockAvailable(reservedQuantity)
                truckDeposit = null
                secondDepositCity = null
            }

            Location.SECOND -> {
                truckDeposit!!.addStockAvailable(reservedQuantity)
                firstDepositCity!!.addStockAvailable(reservedQuantity)
                truckDeposit = null
                firstDepositCity = null
            }
        }

        status = Status.APPROVED.name
    }

    fun rejectReservation(location: Location) {
        when (location) {
            Location.TRUCK -> {
                truckDeposit!!.addStockAvailable(reservedQuantity)
                truckDeposit = null
            }

            Location.FIRST -> {
                firstDepositCity!!.addStockAvailable(reservedQuantity)
                firstDepositCity = null
            }

            Location.SECOND -> {
                secondDepositCity!!.addStockAvailable(reservedQuantity)
                secondDepositCity = null
            }
        }

        checkReservations()
    }

    private fun checkReservations() {
        if (truckDeposit != null && firstDepositCity != null && secondDepositCity != null) {
            status = Status.REJECTED.name
        }
    }

    fun setQuantityCompleted(quantityCompleted: Int) {
        this.quantityCompleted = quantityCompleted.toDouble()

        if (truckDeposit != null) truckDeposit!!.removeStockQuantity(quantityCompleted)
        else if(firstDepositCity != null) firstDepositCity!!.removeStockQuantity(quantityCompleted)
        else if(secondDepositCity != null) secondDepositCity!!.removeStockQuantity(quantityCompleted)
    }

    enum class Location {
        TRUCK, FIRST, SECOND
    }
}



