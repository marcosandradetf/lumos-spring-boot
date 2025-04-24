package com.lumos.domain.model

import androidx.room.PrimaryKey

data class Reserve(
    @PrimaryKey val reserveId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val streetId: Long,
    val reserveStatus: String
)
