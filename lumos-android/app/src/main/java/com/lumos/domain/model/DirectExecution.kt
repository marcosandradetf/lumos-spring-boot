package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "direct_reserve", primaryKeys = ["materialStockId", "contractItemId"])
data class DirectReserve(
    val materialStockId: Long, // *_*
    val contractItemId: Long,
    val contractId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val requestUnit: String,
)

@Entity(tableName = "direct_execution")
data class DirectExecution( //tabela nova
    @PrimaryKey val contractId: Long,
    val executionStatus: String,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    var contractor: String,
    var instructions: String? = null,
)

@Entity(tableName = "direct_execution_street",
        indices = [Index(value = ["address"], unique = true)])
data class DirectExecutionStreet( //tabela nova
    @PrimaryKey(autoGenerate = true) val directStreetId: Long = 0,
    var address: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var photoUri: String? = null,
    var deviceId: String,
    val contractId: Long,
    val contractor: String,
    val lastPower: String? = null
)

@Entity(tableName = "direct_execution_street_item")
data class DirectExecutionStreetItem( //tabela nova
    @PrimaryKey(autoGenerate = true) val directStreetItemId: Long = 0,
    val materialStockId: Long,
    val contractItemId: Long,
    var directStreetId: Long = 0,
    val quantityExecuted: Double
)

data class DirectExecutionDTOResponse(
    val contractId: Long,
    val contractor: String,
    val instructions: String?,
    val creationDate: String,
    val reserves: List<DirectReserve>,
)

data class SendDirectExecutionDto(
    val contractId: Long,
    val contractor: String,
    val deviceStreetId: Long,
    val deviceId: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String,
    val lastPower: String?,
    val materials: List<ReservePartial>
)

data class ReservePartial(
    val reserveId: Long = 0,
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    var quantityExecuted: Double,
)
