package com.lumos.domain.model

data class DirectExecutionRequest(
    val directExecutionId: Long,
    val responsible: String? = null,
    val signPath: String? = null,
    val signDate: String? = null,
    val operationalUsers: List<String>? = null
)

data class DirectExecutionStreetRequest(
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
    val finishAt: String?
)

data class ReservePartial(
    val reserveId: Long = 0,
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: String,
    val materialName: String,
    val truckStockControl: Boolean
)

data class ReserveMaterialJoin(
    val reserveId: Long,
    val directExecutionId: Long,
    val materialStockId: Long, // *_*
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String,
    val stockAvailable: String,
    val currentBalance: String? = null,
    val itemName: String? = null,
    val materialBrand: String? = null
)