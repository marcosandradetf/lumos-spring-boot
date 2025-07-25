package com.lumos.lumosspring.execution.repository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JdbcInstallationRepository(
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
//    fun getGroupedMaintenances(): List<Map<String, JsonNode>> {
//        val sql = """
//            -- SUA QUERY COMPLETA AQUI
//            SELECT
//                json_build_object(
//                    'maintenance_id', m.maintenance_id,
//                    'step', de.description,
//                    'streets', json_agg(DISTINCT ms.maintenance_street_id),
//                    'contractor', c.contractor,
//                    'date_of_visit', m.date_of_visit
//                ) AS maintenance,
//                json_build_object(
//                    'electrician', json_build_object(
//                        'name', e.name,
//                        'last_name', e.last_name
//                    ),
//                    'driver', json_build_object(
//                        'name', d.name,
//                        'last_name', d.last_name
//                    )
//                ) AS team
//            FROM direct_execution de
//            JOIN contract c ON c.contract_id = de.contract_id
//            JOIN team t ON t.id_team = de.team_id
//            JOIN app_user e ON t.electrician_id = e.user_id
//            JOIN app_user d ON t.driver_id = d.user_id
//            JOIN direct_execution_street des ON des.direct_execution_id = de.direct_execution_id
//            WHERE de.direct_execution_id = :directExecutionId
//            GROUP BY de.direct_execution_id , c.contractor, e.name, e.last_name, d.name, d.last_name, de.description
//        """.trimIndent()
//
//        return jdbcTemplate.query(sql) { rs, _ ->
//            val maintenanceJson = rs.getString("maintenance")
//            val teamJson = rs.getString("team")
//
//            val maintenanceNode = objectMapper.readTree(maintenanceJson)
//            val teamNode = objectMapper.readTree(teamJson)
//
//            mapOf(
//                "maintenance" to maintenanceNode,
//                "team" to teamNode
//            )
//        }
//    }
//
    fun getDataForReport(executionId: Long): List<Map<String, JsonNode>> {
        val sql = """
            WITH items_by_street AS (
              SELECT
                des.direct_execution_street_id,
                cri.contract_reference_item_id,
                desi.executed_quantity
              FROM direct_execution_street_item desi
              JOIN direct_execution_street des 
                ON des.direct_execution_street_id = desi.direct_execution_street_id
              JOIN contract_item ci 
                ON ci.contract_item_id = desi.contract_item_id
              JOIN contract_reference_item cri 
                ON cri.contract_reference_item_id = ci.contract_item_reference_id
              WHERE des.direct_execution_id = :directExecutionId
            ),
            contract_items AS (
              SELECT 
                ci.unit_price, 
                ci.quantity_executed, 
                cri.contract_reference_item_id,
                cri.description, 
                coalesce(cri.name_for_import, cri.description) as name_for_import
              FROM contract_item ci
              JOIN contract_reference_item cri 
                ON cri.contract_reference_item_id = ci.contract_item_reference_id
              JOIN direct_execution_street_item desi ON desi.contract_item_id = ci.contract_item_id
              JOIN direct_execution_street des on desi.direct_execution_street_id = des.direct_execution_street_id
              WHERE des.direct_execution_id = :directExecutionId
              order by cri.description
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
                'contract_number', c.contract_number,
                'contractor', c.contractor,
                'cnpj', c.cnpj,
                'address', c.address,
                'phone', c.phone
              ) AS contract,
              
                (
                  SELECT json_agg(
                    json_build_object(
                      'description', agg.description,
                      'unit_price', agg.unit_price,
                      'total_price', agg.unit_price * agg.total_quantity,
                      'quantity_executed', agg.total_quantity
                    )
                  )
                  FROM (
                    SELECT 
                      description,
                      unit_price,
                      SUM(quantity_executed) AS total_quantity
                    FROM contract_items
                    GROUP BY description, unit_price
                  ) AS agg
                ) AS values,

              (
            	  SELECT to_json(
            		    ARRAY[
            		      'P.A'
            		    ] || (
            		      SELECT array_agg(cci.name_for_import)
            		      FROM contract_items cci
            		    ) || ARRAY[
            		      'DATA',
            		      'FORNECEDOR'
            		    ]
            	  )
             ) AS columns,

              (
                SELECT json_agg(
                  json_build_array(
                    des.address,
                    'TODO', -- last_power
                    (
                      SELECT json_agg(
                        json_build_array(
                          ci.contract_reference_item_id,
                          COALESCE((
                            SELECT ibs.executed_quantity
                            FROM items_by_street ibs
                            WHERE ibs.direct_execution_street_id = des.direct_execution_street_id
                              AND ibs.contract_reference_item_id = ci.contract_reference_item_id
                            LIMIT 1
                          ), 0)
                        )
                      )
                      FROM contract_items ci
                    ),
                    'TODO', -- street_date
                    'TODO'  -- current_supplier
                  )
                )
                FROM direct_execution_street des
                WHERE des.direct_execution_id = :directExecutionId
              ) AS streets

            FROM direct_execution de
            JOIN contract c ON c.contract_id = de.contract_id
            JOIN company com ON com.id_company = 1
            JOIN team t ON t.id_team = de.team_id
            JOIN app_user e ON t.electrician_id = e.user_id
            JOIN app_user d ON t.driver_id = d.user_id
            WHERE de.direct_execution_id = :directExecutionId;
        """.trimIndent()

        return namedJdbc.query(sql, mapOf("executionId" to executionId)) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val values = objectMapper.readTree(rs.getString("values"))
            val columns = objectMapper.readTree(rs.getString("columns"))
            val streets = objectMapper.readTree(rs.getString("streets"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "values" to values,
                "columns" to columns,
                "streets" to streets
            )
        }
    }


}