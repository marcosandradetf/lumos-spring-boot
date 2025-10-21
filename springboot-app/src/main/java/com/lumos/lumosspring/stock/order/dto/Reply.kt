package com.lumos.lumosspring.stock.order.dto

import java.util.*

data class OrderRequest(val reserveId: Long?, val order: OrderItemRequest)
data class OrderItemRequest(val orderId: UUID?, val materialId: Long)

data class Replies(
    val approved: List<OrderRequest>,
    val rejected: List<OrderRequest>,
)