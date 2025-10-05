package com.lumos.lumosspring.contract.dto

import java.math.BigDecimal

data class ItemResponseDTO(
    val contractItemId: Long,
    val description: String,
    val quantity: BigDecimal,
    val type: String,
    val linking: String?,
    val currentBalance: BigDecimal
)