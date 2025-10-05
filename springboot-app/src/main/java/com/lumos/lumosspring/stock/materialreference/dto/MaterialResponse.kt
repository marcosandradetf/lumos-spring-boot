package com.lumos.lumosspring.stock.materialreference.dto

import java.math.BigDecimal

data class MaterialResponse(
    val idMaterial: Long,
    val materialName: String,
    val materialBrand: String?,
    val materialPower: String?,
    val materialAmps: String?,
    val materialLength: String?,
    val buyUnit: String,
    val requestUnit: String,
    val stockQt: BigDecimal,
    val inactive: Boolean,
    val materialType: String,
    val materialGroup: String,
    val deposit: String,
    val company: String
)
