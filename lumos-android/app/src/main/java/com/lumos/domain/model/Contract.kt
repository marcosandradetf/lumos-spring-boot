package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val itemsIds: String? = null,
    val hasMaintenance: Boolean = false
)