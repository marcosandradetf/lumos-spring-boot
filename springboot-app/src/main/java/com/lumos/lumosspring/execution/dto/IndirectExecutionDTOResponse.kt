package com.lumos.lumosspring.execution.dto

import java.math.BigDecimal
import java.time.Instant

data class DelegateDTO(
    val preMeasurementId: Long,
    val description: String,
    val stockistId: String,
    val preMeasurementStep: Int,
    val street: List<DelegateStreetDTO>,
    val currentUserUUID: String,
)

data class DelegateStreetDTO(
    val preMeasurementStreetId: Long,
    val teamId: Long,
    val prioritized: Boolean,
    val comment: String,
)

data class ExecutionWithoutPreMeasurementItems(
    val contractItemId: Long,
    val quantity: BigDecimal,
)

data class DirectExecutionDTO(
    val contractId: Long,
    val teamId: Long,
    val currentUserUUID: String,
    val stockistId: String,
    val instructions: String?,
    val items: List<ExecutionWithoutPreMeasurementItems>
)

data class MaterialInStockDTO(
    val materialStockId: Long,
    val materialId: Long,
    val materialName: String,
    val materialPower: String?,
    val materialLength: String?,
    val materialType: String,
    val deposit: String,
    val availableQuantity: BigDecimal,
    val requestUnit: String,
)

data class ReserveDTOResponse(
    val description: String,
    val streets: List<ReserveStreetDTOResponse>
)

data class ReserveStreetDTOResponse(
    val preMeasurementStreetId: Long?,
    val directExecutionId: Long?,
    val streetName: String,
    val latitude: Double?,
    val longitude: Double?,
    val prioritized: Boolean,
    val comment: String?,
    val assignedBy: String,
    val teamId: Long,
    val teamName: String,
    val truckDepositName: String,
    val items: List<ItemResponseDTO>,
)

data class ItemResponseDTO(
    val itemId: Long,
    val description: String,
    val quantity: BigDecimal,
    val type: String,
    val linking: String?,
)

//////
data class ReserveDTOCreate(
    val preMeasurementStreetId: Long?,
    val directExecutionId: Long?,
    val teamId: Long,
    val items: List<ReserveItemDTO>
)

data class ReserveItemDTO(
    val itemId: Long,
    val materials: List<ReserveMaterialDTO>
)

data class ReserveMaterialDTO(
    val materialId: Long,
    val depositId: Long,
    val materialQuantity: BigDecimal
)

////
data class ExecutionPartial(
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
    val creationDate: Instant,
    val latitude: Double?,
    val longitude: Double?,
    val contractId: Long,
    val contractor: String,
)

data class IndirectExecutionDTOResponse(
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
    val latitude: Double?,
    val longitude: Double?,
    val contractId: Long,
    val contractor: String,
    val reserves: List<IndirectReserve>,
)

data class DirectExecutionDTOResponse(
    val directExecutionId: Long,
    val currentDirectExecutionId: Long,
    val description: String,
    val instructions: String?,
    val creationDate: String,
    val reserves: List<DirectReserve>,
)

data class IndirectReserve(
    val reserveId: Long, // *_*
    val contractId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: BigDecimal,
    val streetId: Long,
    val requestUnit: String,
    val quantityExecuted: BigDecimal? = null
)

data class DirectReserve(
    val reserveId: Long, // *_*
    val directExecutionId: Long,
    val materialStockId: Long, // *_*
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: BigDecimal,
    val requestUnit: String,
)

data class SendExecutionDto(
    val streetId: Long,
    val reserves: List<ReservePartial>
)

data class ReservePartial(
    val reserveId: Long = 0,
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: BigDecimal,
    val materialName: String
)

data class SendDirectExecutionDto(
    val directExecutionId: Long,
    val description: String,
    val deviceStreetId: Long,
    val deviceId: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String,
    val lastPower: String?,
    val materials: List<ReservePartial>,
    val currentSupply: String?,
    val finishAt: Instant?
)