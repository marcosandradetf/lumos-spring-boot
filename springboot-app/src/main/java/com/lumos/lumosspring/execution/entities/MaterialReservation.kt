package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("material_reservation")
data class MaterialReservation(
    @Id
    var materialIdReservation: Long = 0,
    var description: String? = null,
    var centralMaterialStockId: Long? = null,
    var truckMaterialStockId: Long,
    var preMeasurementStreetId: Long? = null,
    var directExecution: Long? = null,
    var contractItemId: Long,
    var reservedQuantity: Double,
    var quantityCompleted: Double = 0.0,
    var status: String = ReservationStatus.PENDING,
    var teamId: Long,
    )

//fun confirmReservation() {
//    materialStock.removeStockAvailable(reservedQuantity)
//    status = ReservationStatus.APPROVED
//}
//
//fun rejectReservation() {
//    status = ReservationStatus.REJECTED
//}
//
//
//fun markAsCollected() {
//    status = ReservationStatus.COLLECTED
//    materialStock.removeStockQuantity(reservedQuantity)
//    team.deposit.materialStocks.find { it.material.idMaterial == materialStock.material.idMaterial }
//        ?.addStocks(reservedQuantity)
//}



