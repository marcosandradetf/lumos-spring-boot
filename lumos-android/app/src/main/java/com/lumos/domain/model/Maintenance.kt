package com.lumos.domain.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Relation

data class MaintenanceStreetWithItems(
    @Embedded val street: MaintenanceStreet,
    @Relation(
        parentColumn = "maintenanceStreetId",
        entityColumn = "maintenanceStreetId"
    )
    val items: List<MaintenanceStreetItem>
)

@Entity(primaryKeys = ["maintenanceId", "contractId"])
data class Maintenance(
    val maintenanceId: String,
    val contractId: Long,
    val pendingPoints: Boolean,
    var quantityPendingPoints: Int?,
    val dateOfVisit: String,
    val type: String, //rural ou urbana

    val status: String,
    val responsible: String? = null,
    val signPath: String? = null,
    val signDate: String? = null
)

    @Entity(
        primaryKeys = ["maintenanceStreetId", "maintenanceId"],
        indices = [Index(value = ["address", "maintenanceId"], unique = true)]
    )
    data class MaintenanceStreet(
        val maintenanceStreetId: String,
        val maintenanceId: String,

        var address: String,
        var latitude: Double? = null,
        var longitude: Double? = null,
        val comment: String?,
        val lastPower: String?,

        val lastSupply: String?, // n obrigatorio
        val currentSupply: String?, // obrigatorio

        val reason: String?// se led - perguntar qual o problema/motivo daa troca

    )

@Entity(primaryKeys = ["maintenanceId", "maintenanceStreetId", "materialStockId"])
data class MaintenanceStreetItem(
    val maintenanceId: String,
    val maintenanceStreetId: String,
    val materialStockId: Long,
    val quantityExecuted: String = "0",
)

data class MaintenanceJoin(
    val maintenanceId: String,
    val contractId: Long,
    val pendingPoints: Boolean,
    var quantityPendingPoints: Int?,
    val dateOfVisit: String,
    val type: String, //rural ou urbana
    val status: String,
    val contractor: String,
)

