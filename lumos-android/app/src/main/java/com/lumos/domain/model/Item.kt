package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val contractReferenceItemId: Long,
    val description: String,
    val nameForImport: String,
    val type: String?,
    val linking: String?,
    val itemDependency: String?,
)