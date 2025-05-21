package com.lumos.lumosspring.execution.dto

import org.hibernate.annotations.Comment

data class DelegateDTO(
    val preMeasurementId: Long,
    val description: String,
    val stockistId: String,
    val preMeasurementStep: Int,
    val street: List<DelegateStreetDTO>,
    val currentUserUUID: String,
)

data class DelegateStreetDTO(
    val preMeasurementStreetId: Long,
    val teamId: Long,
    val prioritized: Boolean,
    val comment: String,
)
