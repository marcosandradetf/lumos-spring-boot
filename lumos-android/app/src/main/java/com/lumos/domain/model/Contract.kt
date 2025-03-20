package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contracts")
data class Contract(
    @PrimaryKey val contractId: Long,
    val contractor: String,
    val contractFile: String?,
    val createdAt: String,
    var status: String,
)