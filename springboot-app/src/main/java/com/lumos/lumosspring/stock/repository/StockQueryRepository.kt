package com.lumos.lumosspring.stock.repository

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*

@Repository
class StockQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    data class MaterialStockResponse(
        val materialId: Long,
        val materialStockId: Long,
        val materialName: String,
        val specs: String?,
        val stockQuantity: BigDecimal,
        val stockAvailable: BigDecimal,
        val requestUnit: String,
        val type: String,
    )

    data class StockistResponse(
        val stockistId: Long,
        val stockistName: String,
        val stockistPhone: String?,
        val depositId: Long
    )

    data class DepositResponse(
        val depositId: Long,
        val depositName: String,
        val depositAddress: String?,
        val depositPhone: String?,
    )

    data class StockResponse(
        val materialsStock: List<MaterialStockResponse>,
        val deposits: List<DepositResponse>,
        val stockists: List<StockistResponse>,
    )

    fun getMaterialsForMaintenance(depositId: Long): StockResponse {
        val materialStock = jdbcTemplate.query(
            """
                select m.id_material, ms.material_id_stock, m.material_name, coalesce(m.material_power, m.material_length) as specs,
                ms.stock_quantity, ms.stock_available, ms.request_unit, mt.type_name
                from material_stock ms
                inner join material m on m.id_material = ms.material_id
                inner join material_type mt on mt.id_type = m.id_material_type
                where ms.deposit_id = :depositId
            """.trimIndent(),
            mapOf("depositId" to depositId)
        ) { rs, _ ->
            MaterialStockResponse(
                materialId = rs.getLong("id_material"),
                materialStockId = rs.getLong("material_id_stock"),
                materialName = rs.getString("material_name"),
                specs = rs.getString("specs"),
                stockQuantity = rs.getBigDecimal("stock_quantity"),
                stockAvailable = rs.getBigDecimal("stock_available"),
                requestUnit = rs.getString("request_unit"),
                type = rs.getString("type_name"),
            )
        }

        val deposits = jdbcTemplate.query(
            """
                select distinct d.id_deposit, d.deposit_name, d.deposit_address, d.deposit_phone 
                from stockist s 
                join deposit d ON d.id_deposit = s.deposit_id_deposit 
            """.trimIndent()
        ) { rs, _ ->
            DepositResponse(
                depositId = rs.getLong("id_deposit"),
                depositName = rs.getString("deposit_name"),
                depositAddress = rs.getString("deposit_address"),
                depositPhone = rs.getString("deposit_phone"),
            )
        }

        val stockists = jdbcTemplate.query(
            """
                select s.stockist_id, au.name, au.phone_number, s.deposit_id_deposit from stockist s 
                join app_user au on au.user_id = s.user_id_user 
            """.trimIndent()
        ) { rs, _ ->
            StockistResponse(
                stockistId = rs.getLong("stockist_id"),
                stockistName = rs.getString("name"),
                stockistPhone = rs.getString("phone_number"),
                depositId = rs.getLong("deposit_id_deposit"),
            )
        }

        return StockResponse(
            materialsStock = materialStock,
            deposits = deposits,
            stockists = stockists
        )
    }

    fun getTruckDepositId(uuid: UUID): Long? {
        val sql = """
        SELECT deposit_id_deposit FROM team
        WHERE electrician_id = :uuid OR driver_id = :uuid
        LIMIT 1;
    """.trimIndent()

        val params = mapOf("uuid" to uuid)

        return try {
            jdbcTemplate.queryForObject(sql, params, Long::class.java)
        } catch (_: EmptyResultDataAccessException) {
            null
        }
    }

    data class OrderWithItems(
        val orderMaterial: OrderMaterial,
        val items: List<OrderMaterialItem>
    )

    data class OrderMaterial(
        val orderId: String,
        val orderCode: String,
        val createdAt: String,
        val depositId: Long
    )

    data class OrderMaterialItem(
        val orderId: String,
        val materialId: Long,
    )

}