package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.work.Operation.State.IN_PROGRESS

@Entity(tableName = "pre_measurements")
data class PreMeasurement(
    @PrimaryKey(autoGenerate = true) val preMeasurementId: Long = 0,
    val contractID: Long,
    val deviceId: String,
    val status: String,
    val synced: Boolean = false,
)