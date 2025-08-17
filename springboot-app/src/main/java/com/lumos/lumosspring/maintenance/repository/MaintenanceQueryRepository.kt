package com.lumos.lumosspring.maintenance.repository


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.dto.maintenance.MaintenanceStreetItemDTO
import com.lumos.lumosspring.stock.entities.MaterialHistory
import com.lumos.lumosspring.stock.repository.MaterialHistoryRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Repository
class MaintenanceQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
    private val materialHistoryRepository: MaterialHistoryRepository
) {
    fun debitStock(items: List<MaintenanceStreetItemDTO>, maintenanceStreetId: UUID) {
        for (item in items) {
            materialHistoryRepository.save(
                MaterialHistory(
                    materialHistoryId = UUID.randomUUID(),
                    materialStockId = item.materialStockId,
                    maintenanceStreetId = maintenanceStreetId,
                    usedQuantity = item.quantityExecuted,
                    usedDate = Instant.now(),
                    isNewEntry = true
                )
            )

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
            SELECT
              json_build_object(
                'contract_id', c.contract_id,
                'contractor', c.contractor
              ) AS contract,

              json_agg(
                json_build_object(
                  'maintenance_id', m.maintenance_id,
                  'streets', (
                    SELECT json_agg(DISTINCT ms.maintenance_street_id)
                    FROM maintenance_street ms
                    WHERE ms.maintenance_id = m.maintenance_id
                  ),
                  'date_of_visit', m.date_of_visit,
                  'team', json_build_object(
                    'electrician', json_build_object(
                      'name', e.name,
                      'last_name', e.last_name
                    ),
                    'driver', json_build_object(
                      'name', d.name,
                      'last_name', d.last_name
                    )
                  )
                )
                ORDER BY m.maintenance_id
              ) AS maintenances

            FROM maintenance m
            JOIN contract c ON c.contract_id = m.contract_id
            JOIN team t ON t.id_team = m.team_id
            JOIN app_user e ON t.electrician_id = e.user_id
            JOIN app_user d ON t.driver_id = d.user_id
            WHERE m.status = 'FINISHED'
            GROUP BY c.contract_id, c.contractor
            ORDER BY c.contract_id;
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            val contractorJson = rs.getString("contract")
            val maintenanceJson = rs.getString("maintenances")

            val contractNode = objectMapper.readTree(contractorJson)
            val maintenanceNode = objectMapper.readTree(maintenanceJson)

            mapOf(
                "contract" to contractNode,
                "maintenances" to maintenanceNode,
            )
        }
    }

    fun getConventionalMaintenances(maintenanceId: UUID): List<Map<String, JsonNode>> {
        val sql = """
            WITH items_by_street AS (
                SELECT
            msi.maintenance_street_id,
            material_name_unaccent,
            m.material_power
                  FROM maintenance_street_item msi
                  join maintenance_street ms on ms.maintenance_street_id = msi.maintenance_street_id
                  JOIN material_stock mstk ON mstk.material_id_stock = msi.material_stock_id
                  JOIN material m ON m.id_material = mstk.material_id
                  WHERE msi.maintenance_id  = :maintenanceId
                                and ms.last_power is null and ms.reason is null
            )

            SELECT
              json_build_object(
                'social_reason', com.social_reason,
                'company_cnpj', com.company_cnpj,
                'company_address', com.company_address,
                'company_phone', coalesce(com.company_phone, ''),
                'company_logo', com.company_logo,
                'bucket', com.bucket_file_name
              ) AS company,

              json_build_object(
                'contract_number', c.contract_number,
                'contractor', c.contractor,
                'cnpj', c.cnpj,
                'address', c.address,
                'phone', c.phone
              ) AS contract,

              json_build_object(
                'date_of_visit', m.date_of_visit,
                'pending_points', m.pending_points,
                'quantity_pending_points', coalesce(m.quantity_pending_points, 0),
                'type', m.type,
                'responsible', m.responsible,
                'signature_uri', m.signature_uri,
                'sign_date', m.sign_date
              ) AS maintenance,

              execs.executors AS team,

              (
                SELECT json_agg(
                  json_build_object(
                    'address', ms.address,
                    'comment', ms.comment,

                    'relay', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%rele%'
                        AND ibs.material_name_unaccent NOT LIKE '%base%'
                    ) THEN 'X' ELSE '' END,

                    'connection', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%conector%'
                    ) THEN 'X' ELSE '' END,

                    'bulb', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%lampada%'
                        AND ibs.material_name_unaccent NOT LIKE '%sodio%'
                        AND ibs.material_name_unaccent NOT LIKE '%mercurio%'
                    ) THEN 'X' ELSE '' END,

                    'sodium', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%lampada%'
                        AND ibs.material_name_unaccent LIKE '%sodio%'
                    ) THEN 'X' ELSE '' END,

                    'mercury', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%lampada%'
                        AND ibs.material_name_unaccent LIKE '%mercurio%'
                    ) THEN 'X' ELSE '' END,

                    'power', COALESCE((
                      SELECT ibs.material_power
                      FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_power IS NOT NULL
                      LIMIT 1
                    ), ''),

                    'external_reactor', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%reator%'
                        AND ibs.material_name_unaccent LIKE '%externo%'
                    ) THEN 'X' ELSE '' END,

                    'internal_reactor', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%reator%'
                        AND ibs.material_name_unaccent LIKE '%interno%'
                    ) THEN 'X' ELSE '' END,

                    'relay_base', CASE WHEN EXISTS (
                      SELECT 1 FROM items_by_street ibs
                      WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                        AND ibs.material_name_unaccent LIKE '%rele%'
                        AND ibs.material_name_unaccent LIKE '%base%'
                    ) THEN 'X' ELSE '' END
                  )
                )
                FROM maintenance_street ms
                WHERE ms.maintenance_id = m.maintenance_id
                  and ms.last_power is null and ms.reason is null
              ) AS streets,

              (
                SELECT json_build_object(
                  'relay', COUNT(*) FILTER (WHERE material_name_unaccent LIKE '%rele%' AND material_name_unaccent NOT LIKE '%base%'),
                  'connection', COUNT(*) FILTER (WHERE material_name_unaccent LIKE '%conector%'),
                  'bulb', COUNT(*) FILTER (
                    WHERE material_name_unaccent LIKE '%lampada%'
                      AND material_name_unaccent NOT LIKE '%sodio%'
                      AND material_name_unaccent NOT LIKE '%mercurio%'
                  ),
                  'sodium', COUNT(*) FILTER (
                    WHERE material_name_unaccent LIKE '%lampada%' AND material_name_unaccent LIKE '%sodio%'
                  ),
                  'mercury', COUNT(*) FILTER (
                    WHERE material_name_unaccent LIKE '%lampada%' AND material_name_unaccent LIKE '%mercurio%'
                  ),
                  'external_reactor', COUNT(*) FILTER (
                    WHERE material_name_unaccent LIKE '%reator%' AND material_name_unaccent LIKE '%externo%'
                  ),
                  'internal_reactor', COUNT(*) FILTER (
                    WHERE material_name_unaccent LIKE '%reator%' AND material_name_unaccent LIKE '%interno%'
                  ),
                  'relay_base', COUNT(*) FILTER (
                    WHERE material_name_unaccent LIKE '%rele%' AND material_name_unaccent LIKE '%base%'
                  )
                )
                FROM items_by_street
              ) AS total_by_item

            FROM maintenance m
            JOIN contract c ON c.contract_id = m.contract_id
            JOIN company com ON com.id_company = 1
            LEFT JOIN LATERAL (
            SELECT json_agg(
                   json_build_object(
                        'name', au.name,
                        'last_name', au.last_name,
                        'role', r.role_name
                        )
                   ) AS executors
                FROM (
                    SELECT DISTINCT ON (au.user_id)
                           au.name,
                           au.last_name,
                           r.role_name
                    FROM maintenance_executors me
                    JOIN app_user au ON au.user_id = me.user_id
                    JOIN user_role ur ON ur.id_user = au.user_id
                    JOIN role r ON r.role_id = ur.id_role
                    WHERE me.maintenance_id = m.maintenance_id
                    ORDER BY au.user_id, r.role_name ASC
                ) t
            ) execs ON TRUE
            WHERE m.maintenance_id = :maintenanceId;
        """.trimIndent()

        return jdbcTemplate.query(sql, mapOf("maintenanceId" to maintenanceId)) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val maintenance = objectMapper.readTree(rs.getString("maintenance"))
            val streets = objectMapper.readTree(rs.getString("streets"))
            val team = objectMapper.readTree(rs.getString("team"))
            val total_by_item = objectMapper.readTree(rs.getString("total_by_item"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "maintenance" to maintenance,
                "streets" to streets,
                "team" to team,
                "total_by_item" to total_by_item
            )
        }
    }

    fun getLedMaintenances(maintenanceId: UUID): List<Map<String, JsonNode>> {
        val sql = """
            WITH items_by_street AS (
          SELECT
            msi.maintenance_street_id,
            material_name_unaccent,
            m.material_power
          FROM maintenance_street_item msi
          join maintenance_street ms on ms.maintenance_street_id = msi.maintenance_street_id
          JOIN material_stock mstk ON mstk.material_id_stock = msi.material_stock_id
          JOIN material m ON m.id_material = mstk.material_id
          WHERE msi.maintenance_id  = :maintenanceId
                and (ms.last_power is not null or ms.reason is not null)
        )

        SELECT
          json_build_object(
            'social_reason', com.social_reason,
            'company_cnpj', com.company_cnpj,
            'company_address', com.company_address,
            'company_phone', com.company_phone,
            'company_logo', com.company_logo,
            'bucket', com.bucket_file_name
          ) AS company,

          json_build_object(
            'contract_number', COALESCE(c.contract_number, ''),
            'contractor', c.contractor,
            'cnpj', c.cnpj,
            'address', c.address,
            'phone', c.phone
          ) AS contract,

          json_build_object(
            'date_of_visit', m.date_of_visit,
            'pending_points', m.pending_points,
            'quantity_pending_points', coalesce(m.quantity_pending_points, 0),
            'type', m.type,
            'responsible', m.responsible,
            'signature_uri', m.signature_uri,
            'sign_date', m.sign_date
          ) AS maintenance,

          execs.executors AS team,
          (
            SELECT json_agg(
              json_build_object(
                'address', ms.address,
                'relay', CASE WHEN EXISTS (
                  SELECT 1 FROM items_by_street ibs
                  WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                    AND ibs.material_name_unaccent LIKE '%rele%'
                    AND ibs.material_name_unaccent NOT LIKE '%base%'
                ) THEN 'X' ELSE '' END,

                'connection', CASE WHEN EXISTS (
                  SELECT 1 FROM items_by_street ibs
                  WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                    AND ibs.material_name_unaccent LIKE '%conector%'
                ) THEN 'X' ELSE '' END,

                'comment', ms.comment,
                'last_supply', ms.last_supply,
                'current_supply', ms.current_supply,
                'last_power', ms.last_power,
                'power', COALESCE((
                  SELECT ibs.material_power
                  FROM items_by_street ibs
                  WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                    AND ibs.material_power IS NOT NULL
                  LIMIT 1
                  ), ''),

                  'reason', coalesce(ms.reason, '')
              )
            )
            FROM maintenance_street ms
            WHERE ms.maintenance_id = m.maintenance_id
              and (ms.last_power is not null or ms.reason is not null)
          ) AS streets,

          (
            SELECT json_build_object(
              'relay', COUNT(*) FILTER (WHERE material_name_unaccent LIKE '%rele%' AND material_name_unaccent NOT LIKE '%base%'),
              'connection', COUNT(*) FILTER (WHERE material_name_unaccent LIKE '%conector%')
            )
            FROM items_by_street
          ) AS total_by_item

        FROM maintenance m
        JOIN contract c ON c.contract_id = m.contract_id
        JOIN company com ON com.id_company = 1
        LEFT JOIN LATERAL (
            SELECT json_agg(
                       json_build_object(
                           'name', au.name,
                           'last_name', au.last_name,
                           'role', r.role_name
                       )
                   ) AS executors
            FROM (
                SELECT DISTINCT ON (au.user_id)
                       au.name,
                       au.last_name,
                       r.role_name
                FROM maintenance_executors me
                JOIN app_user au ON au.user_id = me.user_id
                JOIN user_role ur ON ur.id_user = au.user_id
                JOIN role r ON r.role_id = ur.id_role
                WHERE me.maintenance_id = m.maintenance_id
                ORDER BY au.user_id, r.role_name ASC
            ) t
        ) execs ON TRUE

        WHERE m.maintenance_id = :maintenanceId;
        """.trimIndent()

        return jdbcTemplate.query(sql, mapOf("maintenanceId" to maintenanceId)) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val maintenance = objectMapper.readTree(rs.getString("maintenance"))
            val streets = objectMapper.readTree(rs.getString("streets"))
            val team = objectMapper.readTree(rs.getString("team"))
            val total_by_item = objectMapper.readTree(rs.getString("total_by_item"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "maintenance" to maintenance,
                "streets" to streets,
                "team" to team,
                "total_by_item" to total_by_item
            )
        }
    }


}