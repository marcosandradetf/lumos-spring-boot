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
    val instructions: String,
    val creationDate: String? = Instant.now().toString(),
    val status: String? = "PENDING",

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

    val status: String = "PENDING",
    val installationPhotoUri: String = "",

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

data class ExecutionDTO(
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
    val latitude: Double,
    val longitude: Double,
    val contractId: Long,
    val contractor: String,
    val reserves: List<IndirectReserve>,
)

data class SendExecutionDto(
    val streetId: Long,
    val reserves: List<ReservePartial>
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