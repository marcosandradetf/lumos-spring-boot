package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import java.time.Instant

// delete: indirect_execution, indirect_reserve

@Entity
data class PreMeasurementInstallation(
    @PrimaryKey
    val preMeasurementId: String,
    val contractor: String,
    val instructions: String?,
    val creationDate: String = Instant.now().toString(),
    val status: String = "PENDING",

    val responsible: String? = null,
    val signPath: String? = null,
    val signDate: String? = null,
    val executorsIds: List<String>? = null,
    @Ignore
    val streets: List<PreMeasurementInstallationStreet>
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

    @Ignore
    val items: List<PreMeasurementInstallationItem>,
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

@Entity
data class InstallationView(
    val id: String,
    val type: String,
    val contractId: Long,
    var contractor: String,
    val executionStatus: String,
    val creationDate: String,
    val streetsQuantity: Int,
    val itemsQuantity: Int,
)