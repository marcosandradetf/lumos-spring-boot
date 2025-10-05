package com.lumos.lumosspring.stock.order.dto

import java.math.BigDecimal

data class ReserveDTOCreate(
    val preMeasurementId: Long?,
    val directExecutionId: Long?,
    val teamId: Long,
    val items: List<ReserveItemDTO>
)

data class ReserveItemDTO(
    val contractItemId: Long,
    val materials: List<ReserveMaterialDTO>
)

data class ReserveMaterialDTO(
    val centralMaterialStockId: Long? = null,
    val truckMaterialStockId: Long? = null,
    val materialId: Long,
    val materialQuantity: BigDecimal
)