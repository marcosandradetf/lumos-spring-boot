package com.lumos.lumosspring.directexecution.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class DirectExecutionDTO(
    val contractId: Long,
    val teamId: Long,
    val currentUserId: UUID,
    val stockistId: UUID,
    val instructions: String?,
    val items: List<ExecutionWithoutPreMeasurementItems>
)


data class ExecutionWithoutPreMeasurementItems(
    val contractItemId: Long,
    val quantity: BigDecimal,
)

data class DirectExecutionDTOResponse(
    val directExecutionId: Long,
    val currentDirectExecutionId: Long,
    val contractId: Long,
    val description: String,
    val instructions: String?,
    val creationDate: String,
    val reserves: List<DirectReserve>,
)

data class DirectReserve(
    val reserveId: Long, // *_*
    val directExecutionId: Long,
    val materialStockId: Long, // *_*
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: BigDecimal,
    val requestUnit: String,
    val currentItemBalance: BigDecimal,
    val currentItemName: String,
)

data class InstallationRequest(
    val directExecutionId: Long,
    val description: String,
    val deviceStreetId: Long,
    val deviceId: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String,
    val lastPower: String?,
    val materials: List<InstallationItemRequest>,
    val currentSupply: String?,
    val finishAt: Instant?,
    val executorsIds: List<UUID>? = null,
)

data class InstallationItemRequest(
    val reserveId: Long,
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: BigDecimal,
    val materialName: String
)
