package com.lumos.lumosspring.maintenance.repository


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.maintenance.dto.MaintenanceStreetItemDTO
import com.lumos.lumosspring.stock.order.installationrequest.model.MaterialHistory
import com.lumos.lumosspring.stock.order.installationrequest.repository.MaterialHistoryRepository
import com.lumos.lumosspring.util.Utils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class MaintenanceQueryRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
    private val materialHistoryRepository: MaterialHistoryRepository
) {
    fun debitStock(items: List<MaintenanceStreetItemDTO>, maintenanceStreetId: UUID) {
        for (item in items) {
            if (item.truckStockControl) {
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
    }

    fun getGroupedMaintenances(
        contractId: Long? = null,
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null,
        type: String? = null
    ): List<Map<String, JsonNode>> {
        var sql = """
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
                  'sign_date', m.sign_date,
                  'team', execs.executors
                )
                ORDER BY m.maintenance_id
              ) AS maintenances

            FROM maintenance m
            JOIN contract c ON c.contract_id = m.contract_id
            LEFT JOIN LATERAL (
            SELECT json_agg(
                   json_build_object(
                        'name', t.name,
                        'last_name', t.last_name,
                        'role', t.role_name
                        )
                   ) AS executors
                FROM (
                    SELECT DISTINCT ON (au.user_id)
                           au.name,
                           au.last_name,
                           r.role_name
                    FROM maintenance_executor me
                    JOIN app_user au ON au.user_id = me.user_id
                    JOIN user_role ur ON ur.id_user = au.user_id
                    JOIN role r ON r.role_id = ur.id_role
                    WHERE me.maintenance_id = m.maintenance_id
                    ORDER BY au.user_id, r.role_name ASC
                ) t
            ) execs ON TRUE
            WHERE m.status = 'FINISHED'
        """.trimIndent()

        sql += if (contractId == null)
            """
                    AND m.tenant_id = :tenantId
                    AND m.date_of_visit >= (now() - INTERVAL '90 day') 
                    AND m.date_of_visit < (now() + INTERVAL '1 day')
                GROUP BY c.contract_id, c.contractor
                ORDER BY c.contractor
            """.trimIndent()
        else
            """
                    AND m.contract_id = :contractId
                    AND m.date_of_visit >= :startDate 
                    AND m.date_of_visit < (:endDate + INTERVAL '1 day')
                    AND EXISTS(
                        SELECT 1
                        FROM maintenance_street_item msi
                        JOIN material_stock ms on ms.material_id_stock = msi.material_stock_id
                        JOIN material mat on mat.id_material = ms.material_id
                        WHERE mat.material_name_unaccent like :type
                            AND msi.maintenance_id = m.maintenance_id
                    )
                GROUP BY c.contract_id, c.contractor, m.date_of_visit
                ORDER BY c.contractor, m.date_of_visit desc;
            """.trimIndent()


        return jdbcTemplate.query(
            sql, mapOf(
                "tenantId" to Utils.getCurrentTenantId(),
                "contractId" to contractId,
                "startDate" to startDate,
                "endDate" to endDate,
                "type" to type
            )
        ) { rs, _ ->
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
                    coalesce(m.material_power, ms.last_power) as material_power
                  FROM maintenance_street_item msi
                  join maintenance_street ms on ms.maintenance_street_id = msi.maintenance_street_id
                  JOIN material_stock mstk ON mstk.material_id_stock = msi.material_stock_id
                  JOIN material m ON m.id_material = mstk.material_id
                  WHERE msi.maintenance_id  = :maintenanceId
                                and ms.reason is null
            )

            SELECT
              json_build_object(
                'social_reason', com.social_reason,
                'company_cnpj', com.company_cnpj,
                'company_address', com.company_address,
                'company_phone', coalesce(com.company_phone, ''),
                'company_logo', com.company_logo
              ) AS company,

              json_build_object(
                'contract_number', c.contract_number,
                'contractor', c.contractor,
                'cnpj', c.cnpj,
                'address', c.address,
                'phone', COALESCE(c.phone, '')
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
                  and ms.reason is null
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
            JOIN company com ON com.id_company = c.company_id
            LEFT JOIN LATERAL (
            SELECT json_agg(
                   json_build_object(
                        'name', t.name,
                        'last_name', t.last_name,
                        'role', t.role_name
                        )
                   ) AS executors
                FROM (
                    SELECT DISTINCT ON (au.user_id)
                           au.name,
                           au.last_name,
                           r.role_name
                    FROM maintenance_executor me
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
                and ms.reason is not null
        )

        SELECT
          json_build_object(
            'social_reason', com.social_reason,
            'company_cnpj', com.company_cnpj,
            'company_address', com.company_address,
            'company_phone', com.company_phone,
            'company_logo', com.company_logo
          ) AS company,

          json_build_object(
            'contract_number', COALESCE(c.contract_number, ''),
            'contractor', c.contractor,
            'cnpj', c.cnpj,
            'address', c.address,
            'phone', COALESCE(c.phone, '')
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
              and ms.reason is not null
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
        JOIN company com ON com.id_company = c.company_id
        LEFT JOIN LATERAL (
            SELECT json_agg(
                   json_build_object(
                        'name', t.name,
                        'last_name', t.last_name,
                        'role', t.role_name
                        )
                   ) AS executors
                FROM (
                    SELECT DISTINCT ON (au.user_id)
                           au.name,
                           au.last_name,
                           r.role_name
                    FROM maintenance_executor me
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

    fun getGroupedConventionalMaintenances(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        contractId: Long
    ): List<Map<String, JsonNode>> {
        val sql = """
            WITH filtered_maintenance AS (
                SELECT
                    maintenance_id,
                    date_of_visit,
                    pending_points,
                    quantity_pending_points,
                    type,
                    responsible,
                    signature_uri,
                    sign_date
                FROM maintenance
                WHERE contract_id = :contractId
                  AND date_of_visit >= :startDate 
                  AND date_of_visit < (:endDate + INTERVAL '1 day')
                  AND EXISTS(
                      SELECT 1
                      FROM maintenance_street
                      WHERE reason IS NULL
                         AND maintenance_street.maintenance_id = maintenance.maintenance_id
                )
            ),
                 items_by_street AS (
                     SELECT
                         msi.maintenance_street_id,
                         material_name_unaccent,
                         COALESCE(mat.material_power, ms.last_power) AS material_power
                     FROM maintenance_street_item msi
                          JOIN maintenance_street ms ON ms.maintenance_street_id = msi.maintenance_street_id
                          JOIN filtered_maintenance m ON m.maintenance_id = ms.maintenance_id
                          JOIN material_stock mstk ON mstk.material_id_stock = msi.material_stock_id
                          JOIN material mat ON mat.id_material = mstk.material_id
                 ),
                 team_by_maintenance AS (
                     SELECT
                         x.maintenance_id,
                         json_agg(
                                 json_build_object(
                                         'name', x.name,
                                         'last_name', x.last_name,
                                         'role', x.role_name
                                 )
                         ) AS team
                     FROM (
                              SELECT DISTINCT ON (me.maintenance_id, au.user_id)
                                  me.maintenance_id,
                                  au.name,
                                  au.last_name,
                                  r.role_name
                              FROM maintenance_executor me
                                       JOIN filtered_maintenance m ON m.maintenance_id = me.maintenance_id
                                       JOIN app_user au ON au.user_id = me.user_id
                                       JOIN user_role ur ON ur.id_user = au.user_id
                                       JOIN role r ON r.role_id = ur.id_role
                              ORDER BY me.maintenance_id, au.user_id, r.role_name
                          ) x
                     GROUP BY maintenance_id
                 ),
                 streets_by_maintenance AS (
                     SELECT
                         ms.maintenance_id,
                         json_agg(
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
                                         'power', COALESCE((
                                                               SELECT ibs.material_power
                                                               FROM items_by_street ibs
                                                               WHERE ibs.maintenance_street_id = ms.maintenance_street_id
                                                                 AND ibs.material_power IS NOT NULL
                                                               LIMIT 1
                                                           ), '')
                                 )
                         ) AS streets
                     FROM maintenance_street ms
                              JOIN filtered_maintenance m ON m.maintenance_id = ms.maintenance_id
                     WHERE ms.reason IS NULL
                     GROUP BY ms.maintenance_id
                 ),
                 maintenance_json AS (
                     SELECT
                         m.maintenance_id,
                         json_build_object(
                                 'date_of_visit', m.date_of_visit,
                                 'pending_points', m.pending_points,
                                 'quantity_pending_points', COALESCE(m.quantity_pending_points, 0),
                                 'type', m.type,
                                 'responsible', m.responsible,
                                 'signature_uri', m.signature_uri,
                                 'sign_date', m.sign_date,
                                 'team', tbm.team,
                                 'streets', sbm.streets
                         ) AS maintenance
                     FROM filtered_maintenance m
                              LEFT JOIN team_by_maintenance tbm ON tbm.maintenance_id = m.maintenance_id
                              LEFT JOIN streets_by_maintenance sbm ON sbm.maintenance_id = m.maintenance_id
                 )
            
                SELECT json_build_object(
                               'social_reason', com.social_reason,
                               'company_cnpj', com.company_cnpj,
                               'company_address', com.company_address,
                               'company_phone', COALESCE(com.company_phone, ''),
                               'company_logo', com.company_logo
                       )                                                   AS company,
            
                       json_build_object(
                               'contract_number', c.contract_number,
                               'contractor', c.contractor,
                               'cnpj', c.cnpj,
                               'address', c.address,
                               'phone', COALESCE(c.phone, '')
                       )                                                   AS contract,
            
                       json_agg(mj.maintenance ORDER BY mj.maintenance_id) AS maintenances
            
                FROM contract c
                         JOIN company com ON com.id_company = c.company_id
                         LEFT JOIN maintenance_json mj ON TRUE
                WHERE c.contract_id = :contractId
                GROUP BY com.social_reason, com.company_cnpj, com.company_address, com.company_phone, com.company_logo,
                         c.contract_number, c.contractor, c.cnpj, c.address, c.phone;
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            mapOf(
                "contractId" to contractId,
                "startDate" to startDate,
                "endDate" to endDate
            )
        ) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val maintenances = objectMapper.readTree(rs.getString("maintenances"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "maintenances" to maintenances
            )
        }

    }


}