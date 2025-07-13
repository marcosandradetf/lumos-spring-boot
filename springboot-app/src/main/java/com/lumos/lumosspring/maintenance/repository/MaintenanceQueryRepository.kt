package com.lumos.lumosspring.maintenance.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MaintenanceQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    data class MaintenanceDTO(
        val maintenanceId: String,
        val contractId: Long,
        val pendingPoints: Boolean,
        val quantityPendingPoints: Int?,
        val dateOfVisit: String,
        val type: String, //rural ou urbana
        val status: String
    )

    data class MaintenanceStreetWithItems(
        val street: MaintenanceStreetDTO,
        val items: List<MaintenanceStreetItemDTO>
    )

    data class MaintenanceStreetDTO(
        val maintenanceStreetId: String,
        val maintenanceId: String,
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
        val quantityExecuted: Double,
    )

    fun debitStock(items: List<MaintenanceStreetItemDTO>) {
        for (item in items) {
            jdbcTemplate.update(
                """
                update material_stock 
                set stock_quantity = stock_quantity - :quantityExecuted,
                stock_available = stock_available - :quantityExecuted
                where material_id_stock = :materialStockId
            """.trimIndent(),
                mapOf(
                    "materialStockId" to item.materialStockId,
                    "quantityExecuted" to item.quantityExecuted
                )
            )
        }

    }


}