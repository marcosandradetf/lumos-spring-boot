package com.lumos.lumosspring.premeasurement.dto.installation

import java.math.BigDecimal
import java.util.UUID

data class InstallationRequest(
    val streetId: UUID,
    val items: List<InstallationItemRequest>
)

data class InstallationItemRequest(
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: BigDecimal,
    val materialName: String
)

