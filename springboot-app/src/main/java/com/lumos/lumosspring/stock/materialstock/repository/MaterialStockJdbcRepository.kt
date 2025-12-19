package com.lumos.lumosspring.stock.materialstock.repository

import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse
import com.lumos.lumosspring.stock.materialstock.dto.MaterialInStockDTO
import com.lumos.lumosspring.util.Utils
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)


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
            stockQt = rs.getBigDecimal("stockQt"),
            inactive = rs.getBoolean("inactive"),
            materialType = rs.getString("materialType"),
            materialGroup = rs.getString("materialGroup"),
            deposit = rs.getString("deposit")
        )
    }

    fun searchMaterial(name: String, page: Int, size: Int): PagedResponse<MaterialResponse> {
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
            d.deposit_name       AS deposit
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        JOIN material_group mg ON mt.id_group = mg.id_group
        JOIN deposit d ON ms.deposit_id = d.id_deposit
        WHERE (m.material_name_unaccent LIKE :likeName
           OR LOWER(mt.type_name) LIKE :likeName
           OR LOWER(m.material_power) LIKE :likeName
           OR LOWER(m.material_length) LIKE :likeName)
           AND ms.tenant_id = :tenantId
        ORDER BY ms.material_id_stock
        LIMIT :limit OFFSET :offset
    """.trimIndent()


        val offset = page * size
        val likeName = "%${name.lowercase()}%"
        val params = mapOf(
            "likeName" to likeName,
            "limit" to size + 1, // Busca um a mais para saber se tem próxima página
            "offset" to offset,
            "tenantId" to Utils.getCurrentTenantId()
        )

        val content = jdbc.query(sql, params, materialResponseRowMapper)

        val hasNext = content.size > size
        val pagedContent = if (hasNext) content.take(size) else content

        val last = !hasNext
        val totalPages = if (hasNext) page + 2 else page + 1
        val totalElements = if (hasNext) ((page + 1) * size + 1).toLong() else (page * size + content.size).toLong()

        return PagedResponse(
            content = pagedContent,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            last = last
        )

    }


    fun searchMaterialWithDeposit(
        name: String,
        depositId: Long,
        page: Int,
        size: Int
    ): PagedResponse<MaterialResponse> {
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
            d.deposit_name       AS deposit
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        JOIN material_group mg ON mt.id_group = mg.id_group
        JOIN deposit d ON ms.deposit_id = d.id_deposit
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
        val total = jdbc.queryForObject(countSql, params, Long::class.java) ?: 0L

        val totalPages = if (total == 0L) 1 else ((total + size - 1) / size).toInt()  // teto da divisão
        val last = page >= totalPages - 1

        return PagedResponse(
            content = content,
            page = page,
            size = size,
            totalElements = total,
            totalPages = totalPages,
            last = last
        )
    }


    fun findAllByType(type: String, teamId: Long): List<MaterialInStockDTO> {
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
              ms.request_unit AS requestUnit,
              d.is_truck AS isTruck,
              t.plate_vehicle as plateVehicle
            FROM material_stock ms
              JOIN material m ON ms.material_id = m.id_material
              JOIN material_type mt ON m.id_material_type = mt.id_type
              JOIN deposit d ON ms.deposit_id = d.id_deposit
              LEFT JOIN team t on t.deposit_id_deposit = d.id_deposit
            WHERE LOWER(mt.type_name) = :type
              AND ms.inactive = false
              AND (
                t.id_team = :teamId
                OR d.is_truck = false
              )
              AND ms.tenant_id = :tenantId
            ORDER BY  (d.is_truck::int) DESC, d.deposit_name;
        """
        val params = mapOf(
            "type" to type.lowercase(),
            "teamId" to teamId,
            "tenantId" to Utils.getCurrentTenantId()
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
                availableQuantity = rs.getBigDecimal("stockAvailable"),
                requestUnit = rs.getString("requestUnit"),
                isTruck = rs.getBoolean("isTruck"),
                plateVehicle = rs.getString("plateVehicle"),
            )
        }
    }

    fun findAllByLinkingAndType(linking: String, type: String, teamId: Long): List<MaterialInStockDTO> {
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
              ms.request_unit AS requestUnit,
              d.is_truck AS isTruck,
              t.plate_vehicle as plateVehicle
            FROM material_stock ms
              JOIN material m ON ms.material_id = m.id_material
              JOIN material_type mt ON m.id_material_type = mt.id_type
              JOIN deposit d ON ms.deposit_id = d.id_deposit
              LEFT JOIN team t on t.deposit_id_deposit = d.id_deposit
            WHERE LOWER(mt.type_name) = :type
              AND (
                LOWER(m.material_power) = :linking
                OR LOWER(m.material_length) = :linking
              )
              AND ms.inactive = false
              AND (
                t.id_team = :teamId
                OR d.is_truck = false
              )
              AND ms.tenant_id = :tenantId
            ORDER BY  (d.is_truck::int) DESC, d.deposit_name;
        """
        val params = mapOf(
            "linking" to linking.lowercase(),
            "type" to type.lowercase(),
            "teamId" to teamId,
            "tenantId" to Utils.getCurrentTenantId()
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
                availableQuantity = rs.getBigDecimal("stockAvailable"),
                requestUnit = rs.getString("requestUnit"),
                isTruck = rs.getBoolean("isTruck"),
                plateVehicle = rs.getString("plateVehicle")
            )
        }
    }

    fun findAllMaterialsStock(page: Int, size: Int): PagedResponse<MaterialResponse> {
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
                d.deposit_name         AS deposit
            FROM material_stock ms
            JOIN material m ON ms.material_id = m.id_material
            JOIN material_type mt ON m.id_material_type = mt.id_type
            JOIN material_group mg ON mt.id_group = mg.id_group
            JOIN deposit d ON ms.deposit_id = d.id_deposit
            WHERE ms.tenant_id = :tenantId
            ORDER BY ms.material_id_stock
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val countSql = "SELECT COUNT(*) FROM material_stock"

        val offset = page * size
        val params = mapOf(
            "limit" to size, "offset" to offset,
            "tenantId" to Utils.getCurrentTenantId()
        )

        val content = jdbc.query(sql, params, materialResponseRowMapper)
        val total = jdbc.queryForObject(countSql, emptyMap<String, Any>(), Long::class.java) ?: 0L

        val totalPages = if (total == 0L) 1 else ((total + size - 1) / size).toInt()  // teto da divisão
        val last = page >= totalPages - 1

        return PagedResponse(
            content = content,
            page = page,
            size = size,
            totalElements = total,
            totalPages = totalPages,
            last = last
        )
    }

    fun findAllMaterialsStockByDeposit(page: Int, size: Int, depositIdParam: Long): PagedResponse<MaterialResponse> {
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
            d.deposit_name         AS deposit
        FROM material_stock ms
        JOIN material m ON ms.material_id = m.id_material
        JOIN material_type mt ON m.id_material_type = mt.id_type
        JOIN material_group mg ON mt.id_group = mg.id_group
        JOIN deposit d ON ms.deposit_id = d.id_deposit
        WHERE ms.deposit_id = :depositId
        ORDER BY ms.material_id_stock
        LIMIT :limit OFFSET :offset
    """.trimIndent()

        val countSql = """
        SELECT COUNT(*) 
        FROM material_stock ms
        WHERE ms.deposit_id = :depositId
    """.trimIndent()

        val offset = page * size
        val params = mapOf(
            "limit" to size,
            "offset" to offset,
            "depositId" to depositIdParam
        )

        val content = jdbc.query(sql, params, materialResponseRowMapper)
        val total = jdbc.queryForObject(countSql, mapOf("depositId" to depositIdParam), Long::class.java) ?: 0L

        val totalPages = if (total == 0L) 1 else ((total + size - 1) / size).toInt()
        val last = page >= totalPages - 1

        return PagedResponse(
            content = content,
            page = page,
            size = size,
            totalElements = total,
            totalPages = totalPages,
            last = last
        )
    }


    fun insertMaterials(depositId: Long, isTruck: Boolean) {
        var sql = """
                insert into material_stock (
                    buy_unit,
                    cost_per_item,
                    cost_price,
                    inactive,
                    request_unit,
                    stock_available,
                    stock_quantity,
                    deposit_id,
                    material_id,
                    tenant_id
                )
                select
                    'UN',
                    null as cost_per_item,
                    null as cost_price,
                    false as inactive,
                    'UN',
                    0 as stock_available,
                    0 as stock_quantity,
                    :depositId,
                    m.id_material,
                    :tenantId
                from material m
            """

        sql += if (isTruck) {
            """
                join material_type t on t.id_type = m.id_material_type
                where m.is_generic = false 
                    and t.type_name not in ('FITA ISOLANTE AUTOFUSÃO','FITA ISOLANTE ADESIVO','CABO')
            """.trimIndent()
        } else {
            """
                where m.is_generic = false
            """.trimIndent()
        }

        jdbc.update(
            sql,
            mapOf(
                "depositId" to depositId,
                "tenantId" to Utils.getCurrentTenantId()
            )
        );
    }


}
