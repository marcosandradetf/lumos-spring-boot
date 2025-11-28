package com.lumos.domain.model

data class InstallationResponse (
    val preMeasurementId: String,
    val contractId: Long,
    val contractor: String,
    val instructions: String,
    val streets: List<StreetsInstallationResponse>
)

data class StreetsInstallationResponse(
    val preMeasurementId: String,
    val preMeasurementStreetId: String,
    val address: String,
    val priority: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val lastPower: String,
    val photoUrl: String?,
    val photoExpiration: Long?,
    val objectUri: String?,
    val items: List<ItemsInstallationResponse>,
)

data class ItemsInstallationResponse(
    val preMeasurementStreetId: String,
    val materialStockId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String,
    val specs: String?,
    val currentBalance: String,
    val itemName: String
)