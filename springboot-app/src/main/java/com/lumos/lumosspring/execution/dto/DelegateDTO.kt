package com.lumos.lumosspring.execution.dto

data class DelegateDTO(
    val description: String,
    val stockistId: String,
    val references: List<Long>
)

