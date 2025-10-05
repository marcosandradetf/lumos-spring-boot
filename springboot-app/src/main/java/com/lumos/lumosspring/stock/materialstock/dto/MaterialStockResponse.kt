package com.lumos.lumosspring.stock.materialstock.dto

import java.math.BigDecimal

data class MaterialInStockDTO(
    val materialStockId: Long,
    val materialId: Long,
    val materialName: String,
    val materialPower: String?,
    val materialLength: String?,
    val materialType: String,
    val deposit: String,
    val availableQuantity: BigDecimal,
    val requestUnit: String,
    val isTruck: Boolean,
    val plateVehicle: String?,
)
