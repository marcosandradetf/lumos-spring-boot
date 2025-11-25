package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "direct_reserve")
data class DirectReserve(
    @PrimaryKey val reserveId: Long,
    val directExecutionId: Long,
    val materialStockId: Long, // *_*
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String
)

@Entity(tableName = "direct_execution")
data class DirectExecution( //tabela nova
    @PrimaryKey val directExecutionId: Long,
    var contractId: Long?,
    var description: String,
    var instructions: String? = null,
    val executionStatus: String,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,

    val responsible: String? = null,
    val signPath: String? = null,
    val signDate: String? = null,
    val executorsIds: List<String>? = null,
)

@Entity(tableName = "direct_execution_street")
data class DirectExecutionStreet( //tabela nova
    @PrimaryKey(autoGenerate = true) val directStreetId: Long = 0,
    var address: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var photoUri: String? = null,
    var deviceId: String,
    val directExecutionId: Long,
    val description: String,

    val lastPower: String? = null,

    val finishAt: String?,
    val currentSupply: String?,
)

@Entity(tableName = "direct_execution_street_item")
data class DirectExecutionStreetItem( //tabela nova
    @PrimaryKey(autoGenerate = true)
    val directStreetItemId: Long = 0,
    val reserveId: Long,
    val materialStockId: Long,
    val materialName: String,
    val contractItemId: Long,
    var directStreetId: Long = 0,
    val quantityExecuted: String
)

data class DirectExecutionDTOResponse(
    val directExecutionId: Long,
    val contractId: Long,
    val description: String,
    val instructions: String?,
    val creationDate: String,
    val reserves: List<DirectReserveResponse>,
)

data class DirectReserveResponse(
    @PrimaryKey val reserveId: Long,
    val directExecutionId: Long,
    val materialStockId: Long, // *_*
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String,
    val currentItemBalance: String,
    val currentItemName: String,
)