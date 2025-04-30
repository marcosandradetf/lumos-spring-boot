package com.lumos.lumosspring.execution.dto

data class ExecutionDTO(
    val preMeasurementId: Long,
    val firstDepositCityId: Long,
    val secondDepositCityId: Long,
    val teamId: Long,
    val reserves: List<ReserveResponseDTO>,
)
