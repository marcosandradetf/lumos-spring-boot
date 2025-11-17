package com.lumos.lumosspring.directexecution.repository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.common.repository.InstallationReportRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class DirectExecutionReportRepository(
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
): InstallationReportRepository {

    override fun getDataForReport(directExecutionId: Long): List<Map<String, JsonNode>> {
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
                      'description', agg.description,
                      'unit_price', agg.unit_price,
                      'total_price', ROUND(agg.total_quantity * agg.unit_price, 2),
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
             
             execs.executors AS team,
        
             (
				  SELECT json_agg(street_row)
				  FROM (
				    SELECT json_build_array(
				      des.address,
				      coalesce(des.last_power, ''),
				      (
				        SELECT json_agg(
				          CASE 
				            WHEN EXISTS (
				              SELECT 1
				              FROM items_by_street ibs
				              WHERE ibs.direct_execution_street_id = des.direct_execution_street_id
				                AND ibs.contract_item_id = ci.contract_item_id
				            ) THEN (
				              SELECT sum(ibs.executed_quantity)
				              FROM items_by_street ibs
				              WHERE ibs.direct_execution_street_id = des.direct_execution_street_id
				                AND ibs.contract_item_id = ci.contract_item_id
				            )
				            ELSE 0
				          END
				        )
				        FROM items_by_street_distinct ci
				      ),
				      des.finished_at,
				      coalesce(des.current_supply, '')
				    ) AS street_row
				    FROM direct_execution_street des
				    WHERE des.direct_execution_id = :directExecutionId
				    ORDER BY des.finished_at
				  ) AS ordered_rows
			) AS streets,
            
            (
              SELECT json_agg(total_sum)
              FROM (
                SELECT SUM(executed_quantity) AS total_sum
                FROM items_by_street
                GROUP BY contract_item_id, description
                ORDER BY description
              ) AS summed
            ) AS street_sums,
        
            (
                SELECT 
                    json_build_object(
                    'total_price', ROUND(SUM((executed_quantity) * unit_price), 2)
                    )
                FROM items_by_street
            ) AS total
        
        FROM direct_execution de
        JOIN contract c ON c.contract_id = de.contract_id
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
                    FROM direct_execution_executor dee
                    JOIN app_user au ON au.user_id = dee.user_id
                    JOIN user_role ur ON ur.id_user = au.user_id
                    JOIN role r ON r.role_id = ur.id_role
                    WHERE dee.direct_execution_id = de.direct_execution_id
                    ORDER BY au.user_id, r.role_name
                ) t
            ) execs ON TRUE
        WHERE de.direct_execution_id = :directExecutionId;
        """.trimIndent()

        return namedJdbc.query(sql, mapOf("directExecutionId" to directExecutionId)) { rs, _ ->
            val company = objectMapper.readTree(rs.getString("company"))
            val contract = objectMapper.readTree(rs.getString("contract"))
            val values = objectMapper.readTree(rs.getString("values"))
            val columns = objectMapper.readTree(rs.getString("columns"))
            val streets = objectMapper.readTree(rs.getString("streets"))
            val streetSums = objectMapper.readTree(rs.getString("street_sums"))
            val total = objectMapper.readTree(rs.getString("total"))
            val team = objectMapper.readTree(rs.getString("team"))

            mapOf(
                "company" to company,
                "contract" to contract,
                "values" to values,
                "columns" to columns,
                "streets" to streets,
                "street_sums" to streetSums,
                "total" to total,
                "team" to team,
            )
        }
    }

    override fun getDataPhotoReport(directExecutionId: Long): List<Map<String, JsonNode>> {
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

}