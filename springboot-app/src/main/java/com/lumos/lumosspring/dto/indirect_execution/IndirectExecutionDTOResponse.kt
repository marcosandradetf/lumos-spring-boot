package com.lumos.lumosspring.dto.indirect_execution

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class DelegateDTO(
    val preMeasurementId: Long,
    val description: String,
    val stockistId: UUID,
    val preMeasurementStep: Int,
    val street: List<DelegateStreetDTO>,
    val teamId: Long,
    val comment: String,
)

data class DelegateStreetDTO(
    val preMeasurementStreetId: Long,
    val prioritized: Boolean,
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
    val isTruck: Boolean,
    val plateVehicle: String?,
)

data class ReserveDTOResponse(
    val preMeasurementId: Long?,
    val directExecutionId: Long?,
    val description: String,
    val comment: String?,
    val assignedBy: String,
    val teamId: Long,
    val teamName: String,
    val truckDepositName: String,
    val items: List<ItemResponseDTO>,
)

data class ItemResponseDTO(
    val contractItemId: Long,
    val description: String,
    val quantity: BigDecimal,
    val type: String,
    val linking: String?,
    val currentBalance: BigDecimal
)

//////
data class ReserveDTOCreate(
    val preMeasurementId: Long?,
    val directExecutionId: Long?,
    val teamId: Long,
    val items: List<ReserveItemDTO>
)

data class ReserveItemDTO(
    val contractItemId: Long,
    val materials: List<ReserveMaterialDTO>
)

data class ReserveMaterialDTO(
    val centralMaterialStockId: Long? = null,
    val truckMaterialStockId: Long? = null,
    val materialId: Long,
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

