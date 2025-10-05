package com.lumos.lumosspring.stock.order.dto

import java.math.BigDecimal
import java.util.*

data class ReserveItem(val reserveId: Long)
data class OrderItemRequest(val materialId: Long, val quantity: BigDecimal)

data class OrderRequest(
    val orderId: UUID,
    val orderItemRequests: List<OrderItemRequest>,
)

data class Replies(
    val approvedReserves: List<ReserveItem>,
    val rejectedReserves: List<ReserveItem>,

    val approvedOrders: OrderRequest,
    val rejectedOrders: OrderRequest,
)

data class RepliesReserves(
    val approved: List<ReserveItem>,
    val rejected: List<ReserveItem>,
)

data class RepliesOrders(
    val approved: OrderRequest,
    val rejected: OrderRequest,
)