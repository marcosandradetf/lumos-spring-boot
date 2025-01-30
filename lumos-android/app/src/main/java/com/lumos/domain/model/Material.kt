package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class Material(
    @PrimaryKey val materialId: Long,
    val materialName: String?,
    val materialBrand: String?,
    val materialPower: String?,
    val materialAmps: String?,
    val materialLength: String?,
    val requestUnit: String?,
    val stockQt: String?,
    val companyName: String?,
    val depositId: Long
)