package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pre_measurement_street_photos")
data class PreMeasurementStreetPhoto(
    @PrimaryKey var preMeasurementStreetId: Long = 0,
    var contractId: Long,
    var photoUri: String?,
)
