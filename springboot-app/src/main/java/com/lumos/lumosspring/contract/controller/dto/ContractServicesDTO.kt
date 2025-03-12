package com.lumos.lumosspring.contract.controller.dto

import java.math.BigDecimal

data class ContractServicesDTO(
    val id: Long,
    val name: String,
    val quantity: Double?,
    val price: String?,
)
