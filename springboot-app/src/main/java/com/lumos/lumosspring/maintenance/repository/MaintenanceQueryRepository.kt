package com.lumos.lumosspring.maintenance.repository


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository


@Repository
class MaintenanceQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    data class MaintenanceDTO(
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

    fun  getGroupedMaintenances(): List<Map<String, JsonNode>> {
        val sql = """
            -- SUA QUERY COMPLETA AQUI
            SELECT 
                json_build_object(
                    'maintenance_id', m.maintenance_id,
                    'type', CASE 
                        WHEN m2.material_name_unaccent ILIKE '%led%' THEN 'Manutenções em Leds'
                        WHEN m2.material_name_unaccent ILIKE '%lampada%' THEN 'Manutenção Convencional'
                        ELSE 'OUTRO'
                    END,
                    'streets', json_agg(DISTINCT ms.maintenance_street_id),
                    'contractor', c.contractor,
                    'date_of_visit', m.date_of_visit
                ) AS maintenance,
                json_build_object(
                    'electrician', json_build_object(
                        'name', e.name,
                        'last_name', e.last_name
                    ),
                    'driver', json_build_object(
                        'name', d.name,
                        'last_name', d.last_name
                    )
                ) AS team
            FROM maintenance m
            JOIN contract c ON c.contract_id = m.contract_id 
            JOIN team t ON t.id_team = m.team_id 
            JOIN app_user e ON t.electrician_id = e.user_id
            JOIN app_user d ON t.driver_id = d.user_id
            JOIN maintenance_street ms ON ms.maintenance_id = m.maintenance_id 
            JOIN maintenance_street_item msi ON msi.maintenance_street_id = ms.maintenance_street_id
            JOIN material_stock ms2 ON ms2.material_id_stock = msi.material_stock_id 
            JOIN material m2 ON m2.id_material = ms2.material_id
            WHERE m2.material_name_unaccent ILIKE '%led%' OR m2.material_name_unaccent ILIKE '%lampada%'
            GROUP BY m.maintenance_id, c.contractor, e.name, e.last_name, d.name, d.last_name,
                CASE 
                    WHEN m2.material_name_unaccent ILIKE '%led%' THEN 'Manutenções em Leds'
                    WHEN m2.material_name_unaccent ILIKE '%lampada%' THEN 'Manutenção Convencional'
                    ELSE 'OUTRO'
                END
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            val maintenanceJson = rs.getString("maintenance")
            val teamJson = rs.getString("team")

            val maintenanceNode = objectMapper.readTree(maintenanceJson)
            val teamNode = objectMapper.readTree(teamJson)

            mapOf(
                "maintenance" to maintenanceNode,
                "team" to teamNode
            )
        }
    }


}