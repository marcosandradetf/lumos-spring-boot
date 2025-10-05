package com.lumos.lumosspring.directexecution.repository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.directexecution.dto.DirectExecutionDTOResponse
import com.lumos.lumosspring.directexecution.dto.DirectReserve
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Repository
class DirectExecutionViewRepository(
    private val namedJdbc: NamedParameterJdbcTemplate,
    val teamRepository: TeamRepository,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    fun getDirectExecutions(operatorUUID: UUID): List<DirectExecutionDTOResponse> {
        val teamId = teamRepository.getCurrentTeamId(operatorUUID) ?: return emptyList()

        val directExecutions = getDirectExecutionsByTeam(teamId.get())
        val directExecutionsIds = directExecutions.map { it["direct_execution_id"] as Long }

        if (directExecutionsIds.isEmpty()) return emptyList()
        val reservesGrouped = getReservesGroupedByDirectExecution(directExecutionsIds)

        return directExecutions.map { execution ->
            val directExecutionId = execution["direct_execution_id"] as Long
            val reserves = reservesGrouped[directExecutionId] ?: emptyList()

            DirectExecutionDTOResponse(
                directExecutionId = directExecutionId,
                currentDirectExecutionId = directExecutionId,
                description = execution["description"] as String,
                instructions = execution["instructions"] as? String,
                creationDate = (execution["assigned_at"] as Timestamp).toInstant().toString(),
                reserves = reserves
            )
        }
    }



    private fun getDirectExecutionsByTeam(teamId: Long): List<Map<String, Any>> {
        return JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT de.direct_execution_id, de.instructions, de.description, de.assigned_at
                FROM direct_execution de
                WHERE de.team_id = :teamId AND de.direct_execution_status = :status
            """.trimIndent(),
            mapOf("teamId" to teamId, "status" to ExecutionStatus.AVAILABLE_EXECUTION)
        )
    }



    private fun getReservesGroupedByDirectExecution(directExecutionIds: List<Long>): Map<Long, List<DirectReserve>> {
        val raw = JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT mr.material_id_reservation, mr.reserved_quantity, 
                       mr.truck_material_stock_id, mr.central_material_stock_id,
                       mr.direct_execution_id, m.material_name, mr.contract_item_id,
                       m.material_power, m.material_length, ms.request_unit
                FROM material_reservation mr
                INNER JOIN material_stock ms ON ms.material_id_stock = mr.truck_material_stock_id
                INNER JOIN material m ON m.id_material = ms.material_id
                WHERE mr.pre_measurement_id IS NULL
                  AND mr.direct_execution_id IN (:direct_execution_ids)
            """.trimIndent(),
            mapOf("direct_execution_ids" to directExecutionIds)
        )

        return raw.groupBy { it["direct_execution_id"] as Long }
            .mapValues { (_, reservations) ->
                reservations.map { r ->
                    var name = r["material_name"] as String
                    val length = r["material_length"] as String?
                    val power = r["material_power"] as String?
                    if (power != null) {
                        name += " $power"
                    } else if (length != null) {
                        name += " $length"
                    }

                    DirectReserve(
                        reserveId = r["material_id_reservation"] as Long,
                        directExecutionId = 0,
                        materialStockId = r["truck_material_stock_id"] as Long,
                        contractItemId = r["contract_item_id"] as Long,
                        materialName = name,
                        materialQuantity = BigDecimal(r["reserved_quantity"].toString()),
                        requestUnit = r["request_unit"] as? String ?: "UN",
                    )
                }
            }
    }

    fun getGroupedInstallations(): List<Map<String, JsonNode>> {
        val sql = """
            SELECT
              json_build_object(
                'contract_id', c.contract_id,
                'contractor', c.contractor
              ) as contract,
              json_agg(
                json_build_object(
                  'direct_execution_id', de.direct_execution_id,
                  'step', de.description,
                  'description', de.description,
                  'type', 'direct_execution',
                  'team', execs.executors
                )
                ORDER BY de.direct_execution_id
              ) AS steps
            FROM direct_execution de
            JOIN contract c ON c.contract_id = de.contract_id
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
                    FROM direct_execution_executor ex
                    JOIN app_user au ON au.user_id = ex.user_id
                    JOIN user_role ur ON ur.id_user = au.user_id
                    JOIN role r ON r.role_id = ur.id_role
                    WHERE de.direct_execution_id = ex.direct_execution_id
                    ORDER BY au.user_id, r.role_name ASC
                ) t
            ) execs ON TRUE
            WHERE de.direct_execution_status = 'FINISHED'
            GROUP BY c.contract_id, c.contractor
            ORDER BY c.contract_id;            
        """.trimIndent()

        return namedJdbc.query(sql) { rs, _ ->
            val contractorJson = rs.getString("contract")
            val stepsJson = rs.getString("steps")

            val contractNode = objectMapper.readTree(contractorJson)
            val stepsNode = objectMapper.readTree(stepsJson)

            mapOf(
                "contract" to contractNode,
                "steps" to stepsNode
            )
        }
    }

}