package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val materialId: String,
    val materialQuantity: Int,
    val lastPower: String,
    val measurementId: Long,
)
