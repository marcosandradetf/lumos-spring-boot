package com.lumos.lumosspring.execution.dto

data class ReserveForStreetsDTO(
    val preMeasurementId: Long,
    val firstDepositCityId: Long,
    val secondDepositCityId: Long,
    val streets: List<ReserveStreetDTO>
)

data class ReserveStreetDTO(
    val preMeasurementStreetId: Long,
    val teamId: Long,
)