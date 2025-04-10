package com.lumos.lumosspring.execution.dto

data class ReserveDTO(
    val preMeasurementStreetId: Long,
    val depositId: Long,
    val teamId: Long,
    val secondDepositId: Long,
    val thirdDepositId: Long
)
