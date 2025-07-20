package com.thryon.lumos.infrastructure.repository

import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import javax.sql.DataSource


@ApplicationScoped
class MaintenanceRepository  {
    @Inject
    private lateinit var dataSource: DataSource

    data class MaintenanceStreetItemDTO(
        val maintenanceId: String,
        val maintenanceStreetId: String,
        val materialStockId: Long,
        val quantityExecuted: Double,
    )

    fun debitStock(items: List<MaintenanceStreetItemDTO>) {
        dataSource.connection.use { conn ->
            // Para performance, faça tudo numa única transação
            conn.autoCommit = false
            val sql = """
                update material_stock 
                set stock_quantity = stock_quantity - ?,
                    stock_available = stock_available - ?
                where material_id_stock = ?
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                for (item in items) {
                    stmt.setDouble(1, item.quantityExecuted)
                    stmt.setDouble(2, item.quantityExecuted)
                    stmt.setLong(3, item.materialStockId)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
            conn.commit()
        }
    }

    fun getGroupedMaintenances(): List<JsonObject> {
        val result = mutableListOf<JsonObject>()

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

        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        val maintenance = JsonObject(rs.getString("maintenance"))
                        val team = JsonObject(rs.getString("team"))
                        result.add(JsonObject(mapOf("maintenance" to maintenance, "team" to team)))
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }

        return result
    }


}
