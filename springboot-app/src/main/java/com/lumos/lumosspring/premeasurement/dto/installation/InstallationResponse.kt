package com.lumos.lumosspring.premeasurement.dto.installation

import java.math.BigDecimal
import java.util.*

data class InstallationResponse (
    val preMeasurementId: UUID,
    val contractId: Long,
    val contractor: String,
    val instructions: String,
    val streets: List<StreetsInstallationResponse>
)

data class StreetsInstallationResponse(
    val preMeasurementId: UUID,
    val preMeasurementStreetId: UUID,
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
    val preMeasurementStreetId: UUID,
    val materialStockId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: BigDecimal,
    val requestUnit: String,
    val currentBalance: BigDecimal,
    val itemName: String
)