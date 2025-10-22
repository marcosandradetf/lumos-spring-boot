package com.lumos.lumosspring.stock.order.teamrequest.dto

data class RequisitionRequest(
    val depositId: Long,
    val status: String,
    val teamId: Long,
)