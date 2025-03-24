package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.work.Operation.State.IN_PROGRESS

@Entity(tableName = "preMeasurements")
data class PreMeasurement(
    @PrimaryKey(autoGenerate = true) val preMeasurementId: Long = 0,
    val contractID: Long,
    val deviceId: String,
    val status: Status,
    val synced: Boolean = false,
)

object Status {
    const val PENDING = "PEDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
}