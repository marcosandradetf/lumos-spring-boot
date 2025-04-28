package com.lumos.lumosspring.execution.dto

data class ReserveDTO(
    val preMeasurementId: Long,
    val firstDepositCityId: Long,
    val secondDepositCityId: Long,
    val teamId: Long,
)

data class ReserveResponseDTO(
    val materialName: String,
    val materialQuantity: Double,
    val streetId: Long,
    val streetName: String,
    val status: String,
)

data class ReplyReserveDTO(
    val reserveId: Long
)