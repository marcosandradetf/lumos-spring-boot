package com.lumos.lumosspring.stock.materialsku.dto

import java.math.BigDecimal

data class MaterialResponse(
    val materialStockId: Long,
    val materialId: Long,
    val materialName: String,
    val barcode: String?,
    val buyUnit: String,
    val requestUnit: String,
    val stockQuantity: BigDecimal,
    val depositName: String
);