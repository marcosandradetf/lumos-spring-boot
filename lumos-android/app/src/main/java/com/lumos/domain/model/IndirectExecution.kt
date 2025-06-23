package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indirect_execution")
data class IndirectExecution(
    @PrimaryKey val streetId: Long,
    val contractId: Long,
    val streetName: String,
    val streetNumber: String? = null,
    val streetHood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val executionStatus: String,
    val priority: Boolean,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var photoUri: String? = null,
    var contractor: String,
)


@Entity(tableName = "indirect_reserve")
data class IndirectReserve(
    @PrimaryKey val reserveId: Long, // *_*
    val contractId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: Double,
    val streetId: Long,
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
    val priority: Boolean,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double,
    val longitude: Double,
    val contractId: Long,
    val contractor: String,
    val reserves: List<IndirectReserve>,
)

data class SendExecutionDto(
    val streetId: Long,
    val reserves: List<ReservePartial>
)

data class ExecutionHolder(
    val streetId: Long? = null,
    val contractId: Long,
    val streetName: String? = null,
    val streetNumber: String? = null,
    val streetHood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val executionStatus: String,
    val priority: Boolean? = null,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var photoUri: String? = null,
    var contractor: String,
    var instructions: String? = null,
)