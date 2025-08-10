package com.lumos.domain.model

import androidx.room.Entity

@Entity(tableName = "pre_measurement_street_item",
    primaryKeys = ["preMeasurementStreetId", "contractReferenceItemId", "preMeasurementId"])
data class PreMeasurementStreetItem(
    var preMeasurementStreetId: String,
    val contractReferenceItemId: Long,
    val preMeasurementId: String,
    var measuredQuantity: String,
)
