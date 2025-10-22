package com.lumos.lumosspring.stock.order.teamrequest.dto

import java.util.*

data class OrderRequest(val reserveId: Long?, val order: OrderItemRequest)
data class OrderItemRequest(val orderId: UUID?, val materialId: Long)

data class ReplyRequest(
    val approved: List<OrderRequest>,
    val rejected: List<OrderRequest>,
)