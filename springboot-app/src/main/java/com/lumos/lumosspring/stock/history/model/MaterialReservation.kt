package com.lumos.lumosspring.stock.history.model

import com.lumos.lumosspring.authentication.model.TenantEntity
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("material_reservation")
data class MaterialReservation(
    @Id
    var materialIdReservation: Long? = null,
    var description: String? = null,
    var centralMaterialStockId: Long? = null,
    var truckMaterialStockId: Long,
    var preMeasurementId: Long? = null,
    var directExecutionId: Long? = null,
    var contractItemId: Long,
    var reservedQuantity: BigDecimal,
    var quantityCompleted: BigDecimal = BigDecimal.ZERO,
    var status: String = ReservationStatus.PENDING,
    var teamId: Long,
) : TenantEntity()

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



