package com.lumos.lumosspring.premeasurement.dto

import java.util.*

data class DelegateDTO(
    val preMeasurementId: Long,
    val description: String,
    val stockistId: UUID,
    val preMeasurementStep: Int,
    val street: List<DelegateStreetDTO>,
    val teamId: Long,
    val comment: String,
)

data class DelegateStreetDTO(
    val preMeasurementStreetId: Long,
    val prioritized: Boolean,
)