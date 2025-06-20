package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lumos.data.database.ExecutionDao.ReservePartial

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

@Entity(tableName = "direct_execution_street")
data class DirectExecutionStreet( //tabela nova
    @PrimaryKey(autoGenerate = true) val directStreetId: Long,
    val address: String? = null,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var photoUri: String? = null,
)

@Entity(tableName = "direct_execution_street_item")
data class DirectExecutionStreetItem( //tabela nova
    @PrimaryKey(autoGenerate = true) val directStreetItemId: Long,
    val directStreetId: Long,
    val materialId: Long,
    val quantityExecuted: Double
)

data class DirectExecutionDTOResponse(
    val contractId: Long,
    val contractor: String,
    val instructions: String,
    val executionStatus: String,
    val reserves: List<Reserve>,
)

data class SendDirectExecutionDto(
    val directExecutionId: Long,
    val deviceStreetId: Long,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val streetName: String,
    val number: Int,
    val hood: String,
    val city: String,
    val lastPower: String?,
    val reserves: List<ReservePartial>
)