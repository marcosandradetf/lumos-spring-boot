package com.lumos.lumosspring.report.repository.installation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.util.Utils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class InstallationReportRepository(
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {

    fun getDataForReport(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        contractId: Long,
        installationId: Long? = null,
        installationType: String? = null
    ): List<Map<String, JsonNode>> {
        val sql = """
                        WITH filtered_execution AS (
                            SELECT
                                installation_id,
                                installation_type,
                                started_at,
                                sign_date,
                                coalesce(sign_date, finished_at) as finished_at,
                                responsible,
                                signature_uri
                            FROM installation_view
                            ${if (installationId != null) {
                                """
                                    WHERE installation_id = :installationId
                                        AND installation_type = :type
                                """.trimIndent() 
                            } else {
                                """
                                    WHERE contract_id = :contractId
                                        AND started_at >= :startDate
                                        AND started_at <= :endDate
                                """.trimIndent() 
                            }}
                        ),
                             items_by_street AS (
                                 SELECT
                                     isv.installation_id,
                                     isv.installation_type,
                                     isv.installation_street_id,
                                     ci.contract_item_id,
                                     ci.unit_price,
                                     isiv.executed_quantity,
                                     cri.description,
                                     COALESCE(cri.name_for_import, cri.description) AS name_for_import
                                 FROM installation_street_item_view isiv
                                 JOIN installation_street_view isv
                                   ON isv.installation_street_id = isiv.installation_street_id and isv.installation_type = isiv.installation_type
                                 JOIN contract_item ci
                                   ON ci.contract_item_id = isiv.contract_item_id
                                 JOIN contract_reference_item cri
                                   ON cri.contract_reference_item_id = ci.contract_item_reference_id
                                 JOIN filtered_execution e
                                   ON e.installation_id = isv.installation_id and e.installation_type = isv.installation_type
                             ),
                        
                             items_by_street_distinct AS (
                                 SELECT DISTINCT
                                     installation_id,
                                     installation_type,
                                     contract_item_id,
                                     description
                                 FROM items_by_street
                             ),
                        
                             team_by_execution AS (
                                 SELECT
                                     x.installation_id,
                                     x.installation_type,
                                     json_agg(
                                             json_build_object(
                                                     'name', x.name,
                                                     'last_name', x.last_name,
                                                     'role', x.role_name
                                             )
                                     ) AS team
                                 FROM (
                                          SELECT DISTINCT ON (iev.installation_id, au.user_id, iev.installation_type)
                                              iev.installation_id,
                                              iev.installation_type,
                                              au.name,
                                              au.last_name,
                                              r.role_name
                                          FROM installation_executor_view iev
                                                   JOIN filtered_execution e
                                                        ON e.installation_id = iev.installation_id AND e.installation_type = iev.installation_type
                                                   JOIN app_user au
                                                        ON au.user_id = iev.user_id
                                                   JOIN user_role ur
                                                        ON ur.id_user = au.user_id
                                                   JOIN role r
                                                        ON r.role_id = ur.id_role
                                          ORDER BY iev.installation_id, au.user_id, iev.installation_type, r.role_name
                                      ) x
                                 GROUP BY x.installation_id, x.installation_type
                             ),
                        
                             values_by_execution AS (
                                 SELECT
                                     installation_id,
                                     installation_type,
                                     json_agg(
                                             json_build_object(
                                                     'description', description,
                                                     'unit_price', unit_price,
                                                     'quantity_executed', total_quantity,
                                                     'total_price', ROUND(total_quantity * unit_price, 2)
                                             )
                                             ORDER BY description
                                     ) AS values
                                     FROM (
                                          SELECT
                                              installation_id,
                                              installation_type,
                                              description,
                                              unit_price,
                                              SUM(executed_quantity) AS total_quantity
                                          FROM items_by_street
                                          GROUP BY installation_id, installation_type,description, unit_price
                                     ) agg
                                 GROUP BY installation_id, installation_type
                             ),
                             
                             general_values as (
                                SELECT
                                     json_agg(
                                             json_build_object(
                                                     'description', description,
                                                     'unit_price', unit_price,
                                                     'quantity_executed', total_quantity,
                                                     'total_price', ROUND(total_quantity * unit_price, 2)
                                             )
                                             ORDER BY description
                                     ) AS values
                                     FROM (
                                          SELECT
                                              description,
                                              unit_price as unit_price,
                                              SUM(executed_quantity) AS total_quantity
                                          FROM items_by_street
                                          GROUP BY description, unit_price
                                     ) agg
                             ),
                        
                             columns_by_execution AS (
                                 SELECT
                                     installation_id,
                                     installation_type,
                                     to_json(
                                             ARRAY['ENDEREÇO','P.A']
                                                 || array_agg(DISTINCT name_for_import ORDER BY name_for_import)
                                                 || ARRAY['DATA','FORNECEDOR']
                                     ) AS columns
                                 FROM items_by_street
                                 GROUP BY installation_id, installation_type
                             ),
                        
                             streets_by_execution AS (
                                 SELECT
                                     iev.installation_id,
                                     iev.installation_type,
                                     json_agg(
                                             json_build_object(
                                                     'address', iev.address,
                                                     'last_power', iev.last_power,
                                                     'items', (
                                                         SELECT json_agg(qty ORDER BY description)
                                                         FROM (
                                                                  SELECT
                                                                      ibsd.description,
                                                                      COALESCE(SUM(ibs.executed_quantity), 0) AS qty
                                                                  FROM items_by_street_distinct ibsd
                                                                  LEFT JOIN items_by_street ibs
                                                                    ON ibs.installation_street_id = iev.installation_street_id and ibs.installation_type = iev.installation_type
                                                                    AND ibs.contract_item_id = ibsd.contract_item_id
                                                                    AND ibs.installation_id = iev.installation_id
                                                                  WHERE ibsd.installation_id = iev.installation_id and ibsd.installation_type = iev.installation_type
                                                                  GROUP BY ibsd.description
                                                              ) x
                                                     ),
                                                     'finished_at', iev.finished_at,
                                                     'current_supply', COALESCE(iev.current_supply, '')
                                             )
                                             ORDER BY iev.finished_at
                                     ) AS streets
                                 FROM installation_street_view iev
                                 JOIN filtered_execution e
                                    ON e.installation_id = iev.installation_id AND e.installation_type = iev.installation_type
                                 GROUP BY iev.installation_id, iev.installation_type
                             ),
                        
                             execution_json AS (
                                 SELECT
                                     e.installation_id,
                                     e.installation_type,
                                     json_build_object(
                                             'installation_id', e.installation_id,
                                             'installation_type', e.installation_type,
                                             'team', tbe.team,
                                             'sign_date', e.sign_date,
                                             'responsible', e.responsible,
                                             'started_at', e.started_at,
                                             'signature_uri', e.signature_uri,
                                             'finished_at', e.finished_at,
                                             'columns', cbe.columns,
                                             'streets', sbe.streets,
                                             'values', vbe.values
                                     ) AS execution
                                 FROM filtered_execution e
                                 LEFT JOIN team_by_execution tbe
                                    ON tbe.installation_id = e.installation_id and tbe.installation_type = e.installation_type
                                 LEFT JOIN streets_by_execution sbe
                                    ON sbe.installation_id = e.installation_id and sbe.installation_type = e.installation_type
                                 LEFT JOIN values_by_execution vbe
                                    ON vbe.installation_id = e.installation_id and vbe.installation_type = e.installation_type
                                 LEFT JOIN columns_by_execution cbe
                                    ON cbe.installation_id = e.installation_id and cbe.installation_type = e.installation_type
                             )
                        
                        SELECT
                            json_build_object(
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
                            json_agg(ej.execution ORDER BY ej.execution->>'finished_at') AS executions,
                            json_build_object(
                                'values', ANY_VALUE(gv.values)
                            ) AS general_values
                        FROM contract c
                        JOIN company com
                            ON com.id_company = c.company_id
                        LEFT JOIN execution_json ej ON TRUE
                        LEFT JOIN general_values gv ON TRUE
                        WHERE c.contract_id = :contractId
                        GROUP BY com.social_reason, com.company_cnpj, com.company_address, com.company_phone, com.company_logo,
                                 c.contract_number, c.contractor, c.cnpj, c.address, c.phone;
        """.trimIndent()

        return namedJdbc.query(sql,
            mapOf(
                "installationId" to installationId,
                "contractId" to contractId,
                "startDate" to startDate,
                "endDate" to endDate,
                "type" to installationType
            ),
        ) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val executions = objectMapper.readTree(rs.getString("executions"))
            val generalValues = objectMapper.readTree(rs.getString("general_values"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "executions" to executions,
                "general_values" to generalValues,
            )
        }
    }

    fun getDataPhotoReport(directExecutionId: Long): List<Map<String, JsonNode>> {
        val sql = """
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
                'phone', coalesce(c.phone, '')
              ) AS contract,

              (
                SELECT json_agg(
                  json_build_object(
                    'address', x.address,
                    'finished_at', coalesce(to_char(x.finished_at AT TIME ZONE 'America/Sao_Paulo', 'DD/MM/YYYY "às" HH24:MI'), ''),
                    'latitude', x.latitude,
            		'longitude', x.longitude,
                    'execution_photo_uri', coalesce(x.execution_photo_uri, '')
                  )
                )
                FROM (
                  SELECT address, finished_at, latitude, longitude, execution_photo_uri
                  FROM direct_execution_street
                  WHERE direct_execution_id = :directExecutionId
                  ORDER BY finished_at
                ) AS x
              ) AS streets

            FROM direct_execution de
            JOIN contract c ON c.contract_id = de.contract_id
            JOIN company com ON com.id_company = c.company_id
            WHERE de.direct_execution_id = :directExecutionId;
        """.trimIndent()

        return namedJdbc.query(sql, mapOf("directExecutionId" to directExecutionId)) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val streets = objectMapper.readTree(rs.getString("streets"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "streets" to streets
            )
        }
    }

    fun getInstallationsData(contractId: Long?, startDate: OffsetDateTime?, endDate: OffsetDateTime?, installationId: Long? = null):List<Map<String, JsonNode>>{
        val sql = """
            SELECT
              json_build_object(
                'contract_id', c.contract_id,
                'contractor', c.contractor
              ) AS contract,

              json_agg(
                json_build_object(
                  'execution_id', iv.installation_id,
                  'execution_type', iv.installation_type,
                  'streets', (
                    SELECT json_agg(DISTINCT ROW(isv.installation_street_id, isv.installation_type))
                    FROM installation_street_view isv
                    WHERE isv.installation_id = iv.installation_id AND isv.installation_type = iv.installation_type
                  ),
                  'date_of_visit', iv.started_at,
                  'finished_at', iv.finished_at,
                  'team', execs.executors
                )
                ORDER BY iv.installation_id
              ) AS executions

            FROM installation_view iv
            JOIN contract c ON c.contract_id = iv.contract_id
            LEFT JOIN LATERAL (
            SELECT json_agg(
                   json_build_object(
                        'name', t.name,
                        'last_name', t.last_name,
                        'role', t.role_name
                        )
                   ) AS executors
                FROM (
                    SELECT DISTINCT ON (au.user_id, iev.installation_type)
                           au.name,
                           au.last_name,
                           r.role_name
                    FROM installation_executor_view iev
                    JOIN app_user au ON au.user_id = iev.user_id
                    JOIN user_role ur ON ur.id_user = au.user_id
                    JOIN role r ON r.role_id = ur.id_role
                    WHERE iv.installation_id = iev.installation_id AND iv.installation_type = iev.installation_type
                    ORDER BY au.user_id, iev.installation_type, r.role_name ASC
                ) t
            ) execs ON TRUE
            WHERE iv.status = 'FINISHED'
            ${if(contractId == null) {
                """
                            AND iv.tenant_id = :tenantId
                            AND iv.started_at >= (now() - INTERVAL '31 day') 
                            AND iv.started_at < (now() + INTERVAL '1 day')
                        GROUP BY c.contract_id, c.contractor
                        ORDER BY c.contractor
                    """.trimIndent()
            } else {
                """
                            AND iv.contract_id = :contractId
                            AND iv.started_at >= :startDate 
                            AND iv.started_at <= :endDate
                        GROUP BY c.contract_id, c.contractor, iv.started_at
                        ORDER BY c.contractor, iv.started_at desc;
                    """.trimIndent()
            }}
        """.trimIndent()

        return namedJdbc.query(
            sql, mapOf(
                "tenantId" to Utils.getCurrentTenantId(),
                "contractId" to contractId,
                "startDate" to startDate,
                "endDate" to endDate,
            )
        ) { rs, _ ->
            val contractorJson = rs.getString("contract")
            val executionsJson = rs.getString("executions")

            val contractNode = objectMapper.readTree(contractorJson)
            val executionNode = objectMapper.readTree(executionsJson)

            mapOf(
                "contract" to contractNode,
                "executions" to executionNode,
            )
        }
    }

}