package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val measurementId: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val depositId: Long,
    val deviceId: String,
    val synced: Boolean = false
)