package com.lumos.domain.model

import androidx.room.PrimaryKey

data class Execution(
    @PrimaryKey val reserveId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val reserveStatus: String,
    
    val streetId: Long,
    val streetName: String,
    val executionStatus: String,
)


