package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "pre_measurement")
data class PreMeasurement(
    @PrimaryKey
    val preMeasurementId: String,
    var contractId: Long,
    var contractor: String,
    var status: String = "IN_PROGRESS",
    var startedAt: String = Instant.now().toString()
)

@Entity(tableName = "pre_measurement_street")
data class PreMeasurementStreet(
    @PrimaryKey
    val preMeasurementStreetId: String,
    val preMeasurementId: String,
    var lastPower: String?,
    var latitude: Double?,
    var longitude: Double?,
    var address: String?,
    var photoUri: String?,
    var status: String? = "MEASURED",
)