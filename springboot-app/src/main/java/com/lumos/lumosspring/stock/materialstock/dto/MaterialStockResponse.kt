package com.lumos.lumosspring.stock.materialstock.dto

import java.math.BigDecimal

data class MaterialInStockDTO(
    val materialStockId: Long,
    val materialId: Long,
    val materialName: String,
    val depositName: String,
    val stockAvailable: BigDecimal,
    val requestUnit: String,
    val isTruck: Boolean,
    val plateVehicle: String?,
    val contractReferenceItemId: Long,
)
