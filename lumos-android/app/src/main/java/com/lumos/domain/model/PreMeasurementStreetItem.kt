package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preMeasurementsStreetItems")
data class PreMeasurementStreetItem(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    var preMeasurementStreetId: Long,
    val materialId: Long,
    val materialQuantity: Int,
)
