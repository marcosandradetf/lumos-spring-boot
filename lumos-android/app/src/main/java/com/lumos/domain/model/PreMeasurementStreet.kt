package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pre_measurement_streets")
data class PreMeasurementStreet(
    @PrimaryKey(autoGenerate = true) val preMeasurementStreetId: Long = 0,
    val preMeasurementId: Long,
    var lastPower: String?,
    var latitude: Double,
    var longitude: Double,
    var street: String,
    var neighborhood: String,
//    var address: String?,
    var number: String?,
    var city: String,
    var state: String?,
    val deviceId: String,
)