package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class PreMeasurementInstallation(
    @PrimaryKey
    val preMeasurementId: String,
    var contractId: Long,
    val contractor: String,
    val instructions: String?,
    val creationDate: String = Instant.now().toString(),
    val status: String = "PENDING",
    val responsible: String? = null,
    val signPath: String? = null,
    val signDate: String? = null,
    val executorsIds: List<String>? = null,
)

@Entity
data class PreMeasurementInstallationStreet(
    @PrimaryKey
    val preMeasurementStreetId: String,
    val preMeasurementId: String,
    val address: String,
    val priority: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val lastPower: String,
    val photoUrl: String?,
    val photoExpiration: Long?,
    val objectUri: String?,

    val status: String = "PENDING",
    val installationPhotoUri: String? = null,
    val currentSupply: String? = null,
)

@Entity(primaryKeys = ["preMeasurementStreetId", "materialStockId", "contractItemId"])
data class PreMeasurementInstallationItem(
    val preMeasurementStreetId: String,
    val materialStockId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String,
    val specs: String?,
    val executedQuantity: String = "0",
)

data class InstallationView(
    val id: String,
    val type: String,
    var contractId: Long? = null,
    var contractor: String,
    val executionStatus: String,
    val creationDate: String,
    val streetsQuantity: Int,
    val itemsQuantity: Int,
    val instructions: String? = null,
)

data class ItemView(
    val preMeasurementStreetId: String,
    val materialStockId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String,
    val specs: String?,
    val stockQuantity: String = "0",
    val executedQuantity: String = "0",
    val currentBalance: String = "0",
    val itemName: String = ""
)