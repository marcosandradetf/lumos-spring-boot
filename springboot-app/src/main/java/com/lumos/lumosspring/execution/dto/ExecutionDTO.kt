package com.lumos.lumosspring.execution.dto

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
    val quantity: Double,
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
    val materialId: Long,
    val materialName: String,
    val materialPower: String?,
    val materialLength: String?,
    val materialType: String,
    val deposit: String,
    val availableQuantity: Double,
    val requestUnit: String,
)

data class ReserveDTOResponse(
    val description: String,
    val streets: List<ReserveStreetDTOResponse>
)

data class ReserveStreetDTOResponse(
    val preMeasurementStreetId: Long,
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
    val quantity: Double,
    val type: String,
    val linking: String?,
)

//////
data class ReserveDTOCreate(
    val preMeasurementStreetId: Long,
    val items: List<ReserveItemDTO>
)

data class ReserveItemDTO(
    val itemId: Long,
    val materials: List<ReserveMaterialDTO>
)

data class ReserveMaterialDTO (
    val materialId: Long,
    val materialQuantity: Double
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
    val latitude: Double?,
    val longitude: Double?,
    val contractId: Long,
    val contractor: String,
    val reserves: List<Reserve>,
)

data class Reserve(
    val reserveId: Long,
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
)

data class SendExecutionDto(
    val streetId: Long,
    val reserves: List<ReservePartial>
)

data class ReservePartial(
    val reserveId: Long,
    val quantityExecuted: Double,
)