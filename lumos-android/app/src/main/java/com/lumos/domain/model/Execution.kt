package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lumos.data.database.ExecutionDao.ReservePartial
import okhttp3.Address

@Entity(tableName = "executions")
data class Execution(
    @PrimaryKey val streetId: Long,

    val streetName: String,
    val streetNumber: String? = null,
    val streetHood: String? = null,
    val city: String? = null,
    val state: String? = null,

    val teamName: String,
    val executionStatus: String,
    val priority: Boolean,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var photoUri: String? = null,
    var contractId: Long,
    var contractor: String,
)

@Entity(tableName = "reserves")
data class Reserve(
    @PrimaryKey val reserveId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val reserveStatus: String,
    val streetId: Long,
    val depositId: Long,
    val depositName: String,
    val depositAddress: String,
    val stockistName: String,
    val phoneNumber: String,
    val requestUnit: String,

    val quantityExecuted: Double? = null
)

data class ExecutionDTO(
    val streetId: Long,
    val streetName: String,
    val streetNumber: String? = null,
    val streetHood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val teamName: String,
    val priority: Boolean,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double,
    val longitude: Double,
    val contractId: Long,
    val contractor: String,
    val reserves: List<Reserve>,
)

data class SendExecutionDto(
    val streetId: Long,
    val reserves: List<ReservePartial>
)

