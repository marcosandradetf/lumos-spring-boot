package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "contracts")
data class Contract(
    @PrimaryKey val contractId: Long,
    val contractor: String,
    val contractFile: String?,
    val createdBy: String,
    val createdAt: String,
    var status: String,
    val startAt: String? = null,
    val deviceId: String? = null,
)