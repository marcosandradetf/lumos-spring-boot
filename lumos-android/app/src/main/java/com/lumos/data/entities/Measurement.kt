package com.lumos.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val items: List<String>, // Use Gson para converter em JSON, se necessário
    val synced: Boolean = false
)