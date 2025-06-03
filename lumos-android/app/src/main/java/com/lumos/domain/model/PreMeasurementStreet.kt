package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pre_measurement_streets")
data class PreMeasurementStreet(
    @PrimaryKey(autoGenerate = true) val preMeasurementStreetId: Long = 0,
    val contractId: Long,
    var lastPower: String?,
    var latitude: Double?,
    var longitude: Double?,
    var street: String,
    var number: String?,
    var neighborhood: String,
    var city: String,
    var state: String?,
    var photoUri: String?
)