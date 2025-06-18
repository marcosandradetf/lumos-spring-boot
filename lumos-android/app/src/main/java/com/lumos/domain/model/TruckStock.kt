package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "truck_stock")
data class TruckStock( //tabela nova
//    @PrimaryKey val contractId: Long,
    @PrimaryKey val materialId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val requestUnit: String,
)

@Entity(tableName = "reserves")
data class Reserve(
    @PrimaryKey val reserveId: Long,
    val materialId: Long, // campo novo
    val materialName: String,
    val materialQuantity: Double,
    val reserveStatus: String,
    val streetId: Long,
    val contractId: Long,
    val depositId: Long,
    val depositName: String,
    val depositAddress: String,
    val stockistName: String,
    val phoneNumber: String,
    val requestUnit: String,
    val quantityExecuted: Double? = null
)
