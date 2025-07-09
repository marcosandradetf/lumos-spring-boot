package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "material_stock")
data class MaterialStock(
    @PrimaryKey val materialIdStock: Long,
    val materialName: String,
    val specs: String?,
    val stockQuantity: Double,
    val stockAvailable: Double,
    val requestUnit: String
)