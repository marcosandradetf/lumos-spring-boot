package com.lumos.lumosspring.premeasurement.dto.installation

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class InstallationStreetRequest(
    val streetId: UUID,
    val currentSupply: String?,
    val lastPower: String?,
    val latitude: Double?,
    val longitude: Double?,
    val items: List<InstallationItemRequest>
)

data class InstallationItemRequest(
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: BigDecimal,
    val materialName: String,
    val truckStockControl: Boolean = true
)

data class InstallationRequest(
    val installationId: UUID,
    val responsible: String? = null,
    val signDate: Instant? = null,
    val operationalUsers: List<UUID>
)