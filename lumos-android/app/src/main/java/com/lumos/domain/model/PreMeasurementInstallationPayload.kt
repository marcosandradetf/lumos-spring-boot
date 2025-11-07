package com.lumos.domain.model

data class InstallationRequest(
    val streetId: String,
    val items: List<InstallationItemRequest>
)

data class InstallationItemRequest(
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: String,
    val materialName: String
)