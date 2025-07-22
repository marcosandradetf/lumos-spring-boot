package com.lumos.lumosspring.contract.dto

data class ContractItemsDTO(
    val id: Long,
    val type: String,
    val length: String?,
    val power: String?,
    val quantity: Double?,
    val price: String?,
    val services: List<ContractServicesDTO>?
)
