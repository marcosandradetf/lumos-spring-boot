package com.lumos.lumosspring.execution.dto

data class ExecutionDTO(
    val streetId: Long,
    val streetName: String,
    val teamId: Long,
    val teamName: String,
    val reserves: List<ReserveResponseDTO>,
)

data class MaterialInStockDTO(
    val materialId: Long,
    val materialName: String,
    val materialPower: String?,
    val materialLength: String?,
    val materialType: String,
    val deposit: String,
    val availableQuantity: Double,
)