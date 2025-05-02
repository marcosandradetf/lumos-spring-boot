package com.lumos.lumosspring.execution.dto

data class ExecutionDTO(
    val streetId: Long,
    val streetName: String,
    val teamId: Long,
    val teamName: String,
    val reserves: List<ReserveResponseDTO>,
)
