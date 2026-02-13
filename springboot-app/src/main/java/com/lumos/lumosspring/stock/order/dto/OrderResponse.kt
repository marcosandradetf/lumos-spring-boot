package com.lumos.lumosspring.stock.order.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class OrderResponse(
    val reserveId: Long?,
    val orderId: UUID?,

    val materialId: Long,

    val requestQuantity: BigDecimal?,
    val stockQuantity: BigDecimal,
    val materialName: String,
    val description: String?,
    val status: String,
)

data class OrdersByCaseResponse(
    val description: String,
    val teamName: String?,
    val orders: List<OrderResponse>
)

data class OrdersByStatusView(
    val contractor: String?,
    val materialIdReservation: Long?,
    val orderId: UUID?,
    val requestQuantity: BigDecimal?,
    val description: String?,
    val materialId: Long,
    val materialName: String,
    val teamName: String,
    val stockQuantity: BigDecimal,
    val status: String,
    val createdAt: Instant?,
)

data class OrdersByKeysView(
    val materialIdReservation: Long?,
    val orderId: UUID?,
    val materialId: Long?,
    val centralMaterialStockId: Long,
    val requestQuantity: BigDecimal?,
    val directExecutionId: Long?,
    val preMeasurementId: Long?,
    val status: String,
    val truckMaterialStockId: Long,
    val materialName: String
)