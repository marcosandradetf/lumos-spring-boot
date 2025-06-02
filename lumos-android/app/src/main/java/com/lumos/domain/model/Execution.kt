package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import okhttp3.Address

@Entity(tableName = "executions")
data class Execution(
    @PrimaryKey val streetId: Long,
    val streetName: String,
    val teamId: Long,
    val teamName: String,
    val executionStatus: String,
    val priority: Boolean,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String? = null,
)

@Entity(tableName = "reserves")
data class Reserve(
    @PrimaryKey val reserveId: Long,
    val materialId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val reserveStatus: String,
    val streetId: Long,
    val depositId: Long,
    val depositName: String,
    val depositAddress: String,
    val stockistName: String,
    val phoneNumber: String,
)

data class ExecutionDTO(
    val streetId: Long,
    val streetName: String,
    val teamId: Long,
    val teamName: String,
    val priority: Boolean,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double,
    val longitude: Double,
    val reserves: List<Reserve>,
)