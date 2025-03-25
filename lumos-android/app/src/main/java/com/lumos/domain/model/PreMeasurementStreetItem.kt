package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pre_measurement_street_items")
data class PreMeasurementStreetItem(
    @PrimaryKey(autoGenerate = true) val preMeasurementItemId: Long = 0,
    var preMeasurementStreetId: Long,
    val materialId: Long,
    val materialQuantity: Int,
)
