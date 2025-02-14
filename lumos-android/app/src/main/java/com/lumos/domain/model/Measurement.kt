package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val measurementId: Long = 0,
    var latitude: Double,
    var longitude: Double,
    var address: String?,
    var number: String?,
    var city: String,
    var depositId: Long,
    val deviceId: String,
    val synced: Boolean = false
)