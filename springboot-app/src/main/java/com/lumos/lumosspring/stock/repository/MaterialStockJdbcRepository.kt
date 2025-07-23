package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.execution.dto.MaterialInStockDTO
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MaterialStockJdbcRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    private val materialResponseRowMapper = RowMapper { rs, _ ->
        MaterialResponse(
            idMaterial = rs.getLong("idMaterial"),
            materialName = rs.getString("materialName"),
            materialBrand = rs.getString("materialBrand"),
            materialPower = rs.getString("materialPower"),
            materialAmps = rs.getString("materialAmps"),
            materialLength = rs.getString("materialLength"),
            buyUnit = rs.getString("buyUnit"),
            requestUnit = rs.getString("requestUnit"),
            stockQt = rs.getDouble("stockQuantity"),
            inactive = rs.getBoolean("inactive"),
            materialType = rs.getString("materialType"),
            materialGroup = rs.getString("materialGroup"),
            deposit = rs.getString("deposit"),
            company = rs.getString("company")
        )
    }

    fun searchMaterial(name: String, page: Int, size: Int): Page<MaterialResponse> {
        val sql = """
        SELECT
            ms.material_id_stock AS idMaterial,
            m.material_name      AS materialName,
            m.material_brand     AS materialBrand,
            m.material_power     AS materialPower,
            m.material_amps      AS materialAmps,
            m.material_length    AS materialLength,
            ms.buy_unit          AS buyUnit,
            ms.request_unit      AS requestUnit,
            ms.stock_quantity    AS stockQt,
            ms.inactive          AS inactive,
            mt.type_name         AS materialType,
            mg.group_name        AS materialGroup,
            d.deposit_name       AS deposit,
            c.fantasy_name       AS company
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        JOIN material_group mg ON mt.id_group = mg.id_group
        JOIN deposit d ON ms.deposit_id = d.id_deposit
        JOIN company c ON ms.company_id = c.id_company
        WHERE m.material_name_unaccent LIKE :likeName
           OR LOWER(mt.type_name) LIKE :likeName
           OR LOWER(m.material_power) LIKE :likeName
           OR LOWER(m.material_length) LIKE :likeName
        ORDER BY ms.material_id_stock
        LIMIT :limit OFFSET :offset
    """.trimIndent()

        val countSql = """
        SELECT COUNT(*) 
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        WHERE m.material_name_unaccent LIKE :likeName
           OR LOWER(mt.type_name) LIKE :likeName
           OR LOWER(m.material_power) LIKE :likeName
           OR LOWER(m.material_length) LIKE :likeName
    """.trimIndent()

        val offset = page * size
        val likeName = "%${name.lowercase()}%"
        val params = mapOf("likeName" to likeName, "limit" to size, "offset" to offset)

        val content = jdbc.query(sql, params, materialResponseRowMapper)
        val total = jdbc.queryForObject(countSql, mapOf("likeName" to likeName), Long::class.java) ?: 0L

        val pageable = PageRequest.of(page, size)
        return PageImpl(content, pageable, total)
    }


    fun searchMaterialWithDeposit(name: String, depositId: Long, page: Int, size: Int): Page<MaterialResponse> {
        val sql = """
        SELECT
            ms.material_id_stock AS idMaterial,
            m.material_name      AS materialName,
            m.material_brand     AS materialBrand,
            m.material_power     AS materialPower,
            m.material_amps      AS materialAmps,
            m.material_length    AS materialLength,
            ms.buy_unit          AS buyUnit,
            ms.request_unit      AS requestUnit,
            ms.stock_quantity    AS stockQt,
            ms.inactive          AS inactive,
            mt.type_name         AS materialType,
            mg.group_name        AS materialGroup,
            d.deposit_name       AS deposit,
            c.fantasy_name       AS company
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        JOIN material_group mg ON mt.id_group = mg.id_group
        JOIN deposit d ON ms.deposit_id = d.id_deposit
        JOIN company c ON ms.company_id = c.id_company
        WHERE ms.deposit_id = :depositId
          AND (
            m.material_name_unaccent LIKE :likeName
            OR LOWER(mt.type_name) LIKE :likeName
            OR LOWER(m.material_power) LIKE :likeName
            OR LOWER(m.material_length) LIKE :likeName
          )
        ORDER BY ms.material_id_stock
        LIMIT :limit OFFSET :offset
    """.trimIndent()

        val countSql = """
        SELECT COUNT(*) 
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        WHERE ms.deposit_id = :depositId
          AND (
            m.material_name_unaccent LIKE :likeName
            OR LOWER(mt.type_name) LIKE :likeName
            OR LOWER(m.material_power) LIKE :likeName
            OR LOWER(m.material_length) LIKE :likeName
          )
    """.trimIndent()

        val offset = page * size
        val likeName = "%${name.lowercase()}%"
        val params = mapOf(
            "depositId" to depositId,
            "likeName" to likeName,
            "limit" to size,
            "offset" to offset
        )

        val content = jdbc.query(sql, params, materialResponseRowMapper)
        val total = jdbc.queryForObject(countSql, mapOf("depositId" to depositId, "likeName" to likeName), Long::class.java) ?: 0L

        val pageable = PageRequest.of(page, size)
        return PageImpl(content, pageable, total)
    }


    fun findAllByType(type: String, depositName: String): List<MaterialInStockDTO> {
        val sql = """
            SELECT 
              ms.material_id_stock AS materialIdStock,
              m.id_material AS materialId,
              m.material_name AS materialName,
              m.material_power AS materialPower,
              m.material_length AS materialLength,
              mt.type_name AS typeName,
              d.deposit_name AS depositName,
              ms.stock_available AS stockAvailable,
              ms.request_unit AS requestUnit
            FROM material_stock ms
              JOIN material m ON ms.material_id = m.id_material
              JOIN material_type mt ON m.id_material_type = mt.id_type
              JOIN deposit d ON ms.deposit_id = d.id_deposit
            WHERE LOWER(mt.type_name) = :type
              AND ms.inactive = false
              AND (
                LOWER(d.deposit_name) = LOWER(:depositName)
                OR LOWER(d.deposit_name) NOT LIKE '%caminhão%'
              )
            ORDER BY d.deposit_name
        """
        val params = mapOf("type" to type.lowercase(), "depositName" to depositName)
        return jdbc.query(sql, params) { rs, _ ->
            MaterialInStockDTO(
                materialStockId = rs.getLong("materialIdStock"),
                materialId = rs.getLong("materialId"),
                materialName = rs.getString("materialName"),
                materialPower = rs.getString("materialPower"),
                materialLength = rs.getString("materialLength"),
                materialType = rs.getString("typeName"),
                deposit = rs.getString("depositName"),
                availableQuantity = rs.getDouble("stockAvailable"),
                requestUnit = rs.getString("requestUnit")
            )
        }
    }

    fun findAllByLinkingAndType(linking: String, type: String, depositName: String): List<MaterialInStockDTO> {
        val sql = """
            SELECT 
              ms.material_id_stock AS materialIdStock,
              m.id_material AS materialId,
              m.material_name AS materialName,
              m.material_power AS materialPower,
              m.material_length AS materialLength,
              mt.type_name AS typeName,
              d.deposit_name AS depositName,
              ms.stock_available AS stockAvailable,
              ms.request_unit AS requestUnit
            FROM material_stock ms
              JOIN material m ON ms.material_id = m.id_material
              JOIN material_type mt ON m.id_material_type = mt.id_type
              JOIN deposit d ON ms.deposit_id = d.id_deposit
            WHERE LOWER(mt.type_name) = :type
              AND (
                LOWER(m.material_power) = :linking
                OR LOWER(m.material_length) = :linking
              )
              AND ms.inactive = false
              AND (
                LOWER(d.deposit_name) = LOWER(:depositName)
                OR LOWER(d.deposit_name) NOT LIKE '%caminhão%'
              )
            ORDER BY d.deposit_name
        """
        val params = mapOf(
            "linking" to linking.lowercase(),
            "type" to type.lowercase(),
            "depositName" to depositName
        )
        return jdbc.query(sql, params) { rs, _ ->
            MaterialInStockDTO(
                materialStockId = rs.getLong("materialIdStock"),
                materialId = rs.getLong("materialId"),
                materialName = rs.getString("materialName"),
                materialPower = rs.getString("materialPower"),
                materialLength = rs.getString("materialLength"),
                materialType = rs.getString("typeName"),
                deposit = rs.getString("depositName"),
                availableQuantity = rs.getDouble("stockAvailable"),
                requestUnit = rs.getString("requestUnit")
            )
        }
    }

    private val rowMapper = RowMapper { rs, _ ->
        MaterialResponse(
            idMaterial       = rs.getLong("idMaterial"),
            materialName     = rs.getString("materialName"),
            materialBrand    = rs.getString("materialBrand"),
            materialPower    = rs.getString("materialPower"),
            materialAmps     = rs.getString("materialAmps"),
            materialLength   = rs.getString("materialLength"),
            buyUnit          = rs.getString("buyUnit"),
            requestUnit      = rs.getString("requestUnit"),
            stockQt          = rs.getDouble("stockQt"),
            inactive         = rs.getBoolean("inactive"),
            materialType     = rs.getString("materialType"),
            materialGroup    = rs.getString("materialGroup"),
            deposit          = rs.getString("deposit"),
            company          = rs.getString("company")
        )
    }

    fun findAllMaterialsStock(page: Int, size: Int): Page<MaterialResponse> {
        val sql = """
            SELECT
                ms.material_id_stock AS idMaterial,
                m.material_name        AS materialName,
                m.material_brand       AS materialBrand,
                m.material_power       AS materialPower,
                m.material_amps        AS materialAmps,
                m.material_length      AS materialLength,
                ms.buy_unit            AS buyUnit,
                ms.request_unit        AS requestUnit,
                ms.stock_quantity      AS stockQt,
                ms.inactive            AS inactive,
                mt.type_name           AS materialType,
                mg.group_name          AS materialGroup,
                d.deposit_name         AS deposit,
                c.fantasy_name         AS company
            FROM material_stock ms
            JOIN material m ON ms.material_id = m.id_material
            JOIN material_type mt ON m.id_material_type = mt.id_type
            JOIN material_group mg ON mt.id_group = mg.id_group
            JOIN deposit d ON ms.deposit_id = d.id_deposit
            JOIN company c ON ms.company_id = c.id_company
            ORDER BY ms.material_id_stock
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val countSql = "SELECT COUNT(*) FROM material_stock"

        val offset = page * size
        val params = mapOf("limit" to size, "offset" to offset)

        val content = jdbc.query(sql, params, rowMapper)
        val total = jdbc.queryForObject(countSql, emptyMap<String, Any>(), Long::class.java) ?: 0L

        val pageable = PageRequest.of(page, size)
        return PageImpl(content, pageable, total)
    }

    fun findAllMaterialsStockByDeposit(page: Int, size: Int, depositId: Long): Page<MaterialResponse> {
        val sql = """
        SELECT
            ms.material_id_stock AS idMaterial,
            m.material_name        AS materialName,
            m.material_brand       AS materialBrand,
            m.material_power       AS materialPower,
            m.material_amps        AS materialAmps,
            m.material_length      AS materialLength,
            ms.buy_unit            AS buyUnit,
            ms.request_unit        AS requestUnit,
            ms.stock_quantity      AS stockQt,
            ms.inactive            AS inactive,
            mt.type_name           AS materialType,
            mg.group_name          AS materialGroup,
            d.deposit_name         AS deposit,
            c.fantasy_name         AS company
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        JOIN material_group mg ON mt.id_group = mg.id_group
        JOIN deposit d ON ms.deposit_id = d.id_deposit
        JOIN company c ON ms.company_id = c.id_company
        WHERE ms.deposit_id = :depositId
        ORDER BY ms.material_id_stock
        LIMIT :limit OFFSET :offset
    """.trimIndent()

        val countSql = "SELECT COUNT(*) FROM material_stock WHERE deposit_id = :depositId"

        val offset = page * size
        val params = mapOf(
            "limit" to size,
            "offset" to offset,
            "depositId" to depositId
        )

        val rowMapper = RowMapper<MaterialResponse> { rs, _ ->
            MaterialResponse(
                idMaterial = rs.getLong("idMaterial"),
                materialName = rs.getString("materialName"),
                materialBrand = rs.getString("materialBrand"),
                materialPower = rs.getString("materialPower"),
                materialAmps = rs.getString("materialAmps"),
                materialLength = rs.getString("materialLength"),
                buyUnit = rs.getString("buyUnit"),
                requestUnit = rs.getString("requestUnit"),
                stockQt = rs.getDouble("stockQt"),
                inactive = rs.getBoolean("inactive"),
                materialType = rs.getString("materialType"),
                materialGroup = rs.getString("materialGroup"),
                deposit = rs.getString("deposit"),
                company = rs.getString("company")
            )
        }

        val content = jdbc.query(sql, params, rowMapper)
        val total = jdbc.queryForObject(countSql, params, Long::class.java) ?: 0L

        val pageable = PageRequest.of(page, size)
        return PageImpl(content, pageable, total)
    }



}
