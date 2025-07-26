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
    fun getDataForReport(directExecutionId: Long): List<Map<String, JsonNode>> {
        val sql = """
            WITH items_by_street AS (
              SELECT
                des.direct_execution_street_id,
                ci.contract_item_id,
                ci.unit_price,
                desi.executed_quantity,
                cri.description, 
                coalesce(cri.name_for_import, cri.description) as name_for_import
              FROM direct_execution_street_item desi
              JOIN direct_execution_street des 
                ON des.direct_execution_street_id = desi.direct_execution_street_id
              JOIN contract_item ci 
                ON ci.contract_item_id = desi.contract_item_id
              JOIN contract_reference_item cri 
                ON cri.contract_reference_item_id = ci.contract_item_reference_id
              WHERE des.direct_execution_id = :directExecutionId
              order by cri.description
            ),
            items_by_street_distinct as (
              select distinct contract_item_id, description 
              FROM items_by_street
              order by description
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
                      'total_price', ROUND(agg.total_quantity::numeric * agg.unit_price, 2),
                      'quantity_executed', agg.total_quantity
                    )
                  )
                  FROM (
                    SELECT 
                      description,
                      unit_price,
                      SUM(executed_quantity) AS total_quantity
                    FROM items_by_street
                    GROUP BY description, unit_price
                    order by description
                  ) AS agg
                ) AS values,
            
              (
                  SELECT to_json(
                        ARRAY[
                          'ENDEREÇO',
                          'P.A'
                        ] || (
                          SELECT array_agg(distinct cci.name_for_import)
                          FROM items_by_street cci
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
                      SELECT string_agg(
                        CASE 
                          WHEN EXISTS (
                            SELECT 1
                            FROM items_by_street ibs
                            WHERE ibs.direct_execution_street_id = des.direct_execution_street_id
                              AND ibs.contract_item_id = ci.contract_item_id
                          ) THEN (
                            SELECT ibs.executed_quantity::text
                            FROM items_by_street ibs
                            WHERE ibs.direct_execution_street_id = des.direct_execution_street_id
                              AND ibs.contract_item_id = ci.contract_item_id
                            LIMIT 1
                          )
                          ELSE '0'
                        END,
                        ', '
                      )
                      FROM items_by_street_distinct ci
                    ),
                    'TODO', -- street_date
                    'TODO'  -- current_supplier
                  )
                )
                FROM direct_execution_street des
                WHERE des.direct_execution_id = :directExecutionId
              ) AS streets,
            
            (
              SELECT string_agg(total_sum::text, ', ')  -- Concatenar as somas
              FROM (
                SELECT SUM(executed_quantity) AS total_sum  -- Soma das quantidades por item
                FROM items_by_street
                GROUP BY contract_item_id,description
                order by description
              ) AS summed
            ) AS street_sums,
        
            (
                SELECT 
                    json_build_object(
                    'total_price', ROUND(SUM((executed_quantity::numeric) * unit_price), 2)
                    )
                FROM items_by_street
            ) AS total
        
        FROM direct_execution de
        JOIN contract c ON c.contract_id = de.contract_id
        JOIN company com ON com.id_company = 1
        JOIN team t ON t.id_team = de.team_id
        JOIN app_user e ON t.electrician_id = e.user_id
        JOIN app_user d ON t.driver_id = d.user_id
        WHERE de.direct_execution_id = :directExecutionId
        """.trimIndent()

        return namedJdbc.query(sql, mapOf("directExecutionId" to directExecutionId)) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val values = objectMapper.readTree(rs.getString("values"))
            val columns = objectMapper.readTree(rs.getString("columns"))
            val streets = objectMapper.readTree(rs.getString("streets"))
            val streetSums = objectMapper.readTree(rs.getString("street_sums"))
            val total = objectMapper.readTree(rs.getString("total"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "values" to values,
                "columns" to columns,
                "streets" to streets,
                "street_sums" to streetSums,
                "total" to total
            )
        }
    }


}