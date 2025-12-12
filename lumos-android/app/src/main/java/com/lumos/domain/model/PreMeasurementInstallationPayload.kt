package com.lumos.domain.model

data class InstallationStreetRequest(
    val streetId: String,
    val currentSupply: String?,
    val lastPower: String?,
    val latitude: Double?,
    val longitude: Double?,
    val items: List<InstallationItemRequest>
)

data class InstallationItemRequest(
    val contractItemId: Long,
    val truckMaterialStockId: Long,
    val quantityExecuted: String,
    val materialName: String
)

data class InstallationRequest(
    val installationId: String,
    val responsible: String? = null,
    val signDate: String? = null,
    val signUri: String? = null,
    val operationalUsers: List<String>
)

data class InstallationStreetPayload(
    val installationPhotoUri: String?,
    val currentSupply: String?,
    val lastPower: String?,
    val latitude: Double?,
    val longitude: Double?
)