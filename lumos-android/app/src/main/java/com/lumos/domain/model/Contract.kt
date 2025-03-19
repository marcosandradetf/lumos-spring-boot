package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contracts")
data class Contract(
    @PrimaryKey val contractId: Long,
    var contractor: String,
    var contractFile: String?,
    var status: String,
)