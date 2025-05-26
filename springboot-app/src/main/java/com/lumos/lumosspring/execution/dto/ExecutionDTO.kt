package com.lumos.lumosspring.execution.dto

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

data class MaterialInStockDTO(
    val materialId: Long,
    val materialName: String,
    val materialPower: String?,
    val materialLength: String?,
    val materialType: String,
    val deposit: String,
    val availableQuantity: Double,
)

data class ReserveDTOResponse(
    val description: String,
    val streets: List<ReserveStreetDTOResponse>
)

data class ReserveStreetDTOResponse(
    val preMeasurementStreetId: Long,
    val streetName: String,
    val latitude: Double,
    val longitude: Double,
    val prioritized: Boolean,
    val comment: String,
    val assignedBy: String,
    val items: List<ItemResponseDTO>,
    val teamName: String,
    val truckDepositName: String
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
    val materialId: Long,
    val materialQuantity: Double
)
