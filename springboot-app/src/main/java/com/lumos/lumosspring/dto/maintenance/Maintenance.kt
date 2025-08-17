package com.lumos.lumosspring.dto.maintenance

import java.math.BigDecimal
import java.util.*

data class SendMaintenanceDTO(
    val maintenanceId: UUID,
    val contractId: Long,
    val pendingPoints: Boolean,
    var quantityPendingPoints: Int?,
    val dateOfVisit: String,
    val type: String, //rural ou urbana

    val status: String,
    val responsible: String? = null,
    val signPath: String? = null,
    val signDate: String? = null,
    val executorsIds: List<UUID>? = null,
)

data class MaintenanceStreetWithItems(
    val street: MaintenanceStreetDTO,
    val items: List<MaintenanceStreetItemDTO>
)

data class MaintenanceStreetDTO(
    val maintenanceStreetId: UUID,
    val maintenanceId: UUID,
    var address: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    val comment: String?,
    val lastPower: String?,

    val lastSupply: String?, // n obrigatorio
    val currentSupply: String?, // obrigatorio
    val reason: String?// se led - perguntar qual o problema/motivo da troca

)

data class MaintenanceStreetItemDTO(
    val maintenanceId: String,
    val maintenanceStreetId: String,
    val materialStockId: Long,
    val quantityExecuted: BigDecimal,
)