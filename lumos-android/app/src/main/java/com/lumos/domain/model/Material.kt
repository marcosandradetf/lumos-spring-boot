package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class Material(
    @PrimaryKey val materialId: Long,
    val materialName: String?,
    val materialPower: String?,
    val materialAmps: String?,
    val materialLength: String?,
)