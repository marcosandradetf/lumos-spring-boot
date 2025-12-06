package com.lumos.lumosspring.contract.dto

import java.math.BigDecimal

data class ContractItemBalance(
    val contractItemId: Long,
    val currentBalance: BigDecimal,
    val itemName: String,
)
