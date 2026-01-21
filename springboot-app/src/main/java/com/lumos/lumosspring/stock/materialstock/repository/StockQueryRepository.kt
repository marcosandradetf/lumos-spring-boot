package com.lumos.lumosspring.stock.materialstock.repository

import com.lumos.lumosspring.util.Utils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal

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

    data class MaterialStockResponseV2(
        val materialId: Long,
        val materialStockId: Long,
        val materialName: String,
        val stockQuantity: BigDecimal,
        val stockAvailable: BigDecimal,
        val requestUnit: String,
        val type: String,
        val truckStockControl: Boolean,
        val parentMaterialId: Long,
        val materialBaseName: String,
        val materialBrand: String?,
        val materialPower: String?,
    )

    data class StockResponseV2(
        val materialsStock: List<MaterialStockResponseV2>,
        val deposits: List<DepositResponse>,
        val stockists: List<StockistResponse>,
    )

    fun getMaterialsForMaintenance(depositId: Long): StockResponse {
        val materialStock = jdbcTemplate.query(
            """
                SELECT m.id_material, ms.material_id_stock, m.material_name, coalesce(m.material_power, m.material_length) AS specs,
                    ms.stock_quantity, ms.stock_available, ms.request_unit, mt.type_name
                FROM material_stock ms
                INNER JOIN material m ON m.id_material = ms.material_id
                INNER JOIN material_type mt ON mt.id_type = m.id_material_type
                WHERE ms.deposit_id = :depositId
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
                WHERE d.tenant_id = :tenantId
            """.trimIndent(),
            mapOf("tenantId" to Utils.getCurrentTenantId())
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
                select s.stockist_id, au.name, au.phone_number, s.deposit_id_deposit 
                from stockist s 
                join app_user au on au.user_id = s.user_id_user
                where au.tenant_id = :tenantId
            """.trimIndent(),
            mapOf("tenantId" to Utils.getCurrentTenantId())
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

    fun getTruckStock(depositId: Long): StockResponseV2 {
        val materialStock = jdbcTemplate.query(
            """
                SELECT
                    m.id_material,
                    ms.material_id_stock,
                    m.material_name,
                    ms.stock_quantity,
                    ms.stock_available,
                    ms.request_unit,
                    mt.type_name,
                    m.truck_stock_control,
                    m.parent_material_id,
                    g.material_name as material_base_name,
                    m.material_brand,
                    m.material_power
                FROM material_stock ms
                    INNER JOIN material m ON m.id_material = ms.material_id
                    INNER JOIN material_type mt ON mt.id_type = m.id_material_type
                    INNER JOIN material g on g.id_material = m.parent_material_id
                WHERE ms.deposit_id = :depositId
            """.trimIndent(),
            mapOf("depositId" to depositId)
        ) { rs, _ ->
            MaterialStockResponseV2(
                materialId = rs.getLong("id_material"),
                materialStockId = rs.getLong("material_id_stock"),
                materialName = rs.getString("material_name"),
                stockQuantity = rs.getBigDecimal("stock_quantity"),
                stockAvailable = rs.getBigDecimal("stock_available"),
                requestUnit = rs.getString("request_unit"),
                type = rs.getString("type_name"),
                truckStockControl = rs.getBoolean("truck_stock_control"),
                parentMaterialId = rs.getLong("parent_material_id"),
                materialBaseName = rs.getString("material_base_name"),
                materialBrand = rs.getString("material_brand"),
                materialPower = rs.getString("material_power"),
            )
        }

        val deposits = jdbcTemplate.query(
            """
                select distinct d.id_deposit, d.deposit_name, d.deposit_address, d.deposit_phone 
                from stockist s 
                join deposit d ON d.id_deposit = s.deposit_id_deposit
                WHERE d.tenant_id = :tenantId
            """.trimIndent(),
            mapOf("tenantId" to Utils.getCurrentTenantId())
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
                select s.stockist_id, au.name, au.phone_number, s.deposit_id_deposit 
                from stockist s 
                join app_user au on au.user_id = s.user_id_user
                where au.tenant_id = :tenantId
            """.trimIndent(),
            mapOf("tenantId" to Utils.getCurrentTenantId())
        ) { rs, _ ->
            StockistResponse(
                stockistId = rs.getLong("stockist_id"),
                stockistName = rs.getString("name"),
                stockistPhone = rs.getString("phone_number"),
                depositId = rs.getLong("deposit_id_deposit"),
            )
        }

        return StockResponseV2(
            materialsStock = materialStock,
            deposits = deposits,
            stockists = stockists
        )
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