package com.lumos.lumosspring.stock.order.dto

data class RequisitionRequest(
    val depositId: Long,
    val status: String,
    val teamId: Long,
)