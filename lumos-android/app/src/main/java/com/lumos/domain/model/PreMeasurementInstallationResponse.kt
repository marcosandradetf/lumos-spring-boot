package com.lumos.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

// delete: indirect_execution, indirect_reserve

@Entity(tableName = "pre_measurement_installation")
data class PreMeasurementInstallation(
    @PrimaryKey
    val preMeasurementId: String,
    val contractor: String,
    val instructions: String,

    @Ignore
    val streets: List<PreMeasurementInstallationStreet>
)

@Entity(tableName = "pre_measurement_installation_street")
data class PreMeasurementInstallationStreet(
    @PrimaryKey
    val preMeasurementStreetId: String,
    val preMeasurementId: String,
    val address: String,
    val priority: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val lastPower: String,


    @Ignore
    val items: List<PreMeasurementInstallationItem>,
)

@Entity(tableName = "pre_measurement_installation_item"
, primaryKeys = ["preMeasurementStreetId", "materialStockId", "contractItemId"]
)
data class PreMeasurementInstallationItem(
    val preMeasurementStreetId: String,
    val materialStockId: Long,
    val contractItemId: Long,
    val materialName: String,
    val materialQuantity: String,
    val requestUnit: String,
    val specs: String?,
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

data class ExecutionHolder(
    val streetId: Long? = null,
    val contractId: Long,
    val streetName: String? = null,
    val streetNumber: String? = null,
    val streetHood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val executionStatus: String,
    val priority: Boolean? = null,
    val type: String,
    val itemsQuantity: Int,
    val creationDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var photoUri: String? = null,
    var contractor: String,
    var instructions: String? = null,
)