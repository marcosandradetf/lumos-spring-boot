package com.lumos.lumosspring.execution.dto

data class ReserveDTO(
    val preMeasurementId: Long,
    val firstDepositCityId: Long,
    val secondDepositCityId: Long,
    val teamId: Long,
)
