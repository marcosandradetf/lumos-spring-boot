package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "executions")
data class Execution(
    @PrimaryKey val streetId: Long,
    val streetName: String,
    val teamId: Long,
    val teamName: String,
    val executionStatus: String,
)

@Entity(tableName = "reserves")
data class Reserve(
    @PrimaryKey val reserveId: Long,
    val materialId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val reserveStatus: String,
    val streetId: Long
)

data class ExecutionDTO(
    val streetId: Long,
    val streetName: String,
    val teamId: Long,
    val teamName: String,
    val reserves: List<Reserve>,
)