package com.lumos.lumosspring.execution.dto

data class LocalStockDTO(
    val streetId: Long,
    val materialsInStock: List<MaterialsInStockDTO>,
    val materialsInTruck: List<MaterialsInStockDTO>
)

data class MaterialsInStockDTO(
    val materialId: Long,
    val materialName: String,
    val deposit: String,
    val availableQuantity: Double,
)
