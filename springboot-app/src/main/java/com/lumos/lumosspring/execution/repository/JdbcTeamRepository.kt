package com.lumos.lumosspring.execution.repository

import com.lumos.lumosspring.execution.dto.DirectExecutionDTOResponse
import com.lumos.lumosspring.execution.dto.DirectReserve
import com.lumos.lumosspring.execution.dto.IndirectExecutionDTOResponse
import com.lumos.lumosspring.execution.dto.IndirectReserve
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Repository
class JdbcGetExecutionRepository(
    private val namedJdbc: NamedParameterJdbcTemplate
) {

    fun getIndirectExecutions(operatorUUID: UUID): List<IndirectExecutionDTOResponse> {
        val teamsId = getTeamsIdsByUser(operatorUUID)
        if (teamsId.isEmpty()) return emptyList()

        val streets = getStreetsByTeams(teamsId)
        val streetIds = streets.map { it["pre_measurement_street_id"] as Long }
        if (streetIds.isEmpty()) return emptyList()

        val reservesGrouped = getReservesGroupedByStreet(streetIds)

        return streets.map { street ->
            val streetId = street["pre_measurement_street_id"] as Long
            val reserves = reservesGrouped[streetId] ?: emptyList()

            IndirectExecutionDTOResponse(
                streetId = streetId,
                streetName = street["street"] as String,
                streetNumber = street["number"] as? String,
                streetHood = street["neighborhood"] as? String,
                city = street["city"] as? String,
                state = street["state"] as? String,
                priority = street["prioritized"] as Boolean,
                type = "INSTALLATION",
                itemsQuantity = reserves.size,
                creationDate = (street["created_at"] as Timestamp).toInstant().toString(),
                latitude = street["latitude"] as? Double,
                longitude = street["longitude"] as? Double,
                contractId = street["contract_id"] as Long,
                contractor = street["contractor"] as String,
                reserves = reserves
            )
        }
    }

    fun getDirectExecutions(operatorUUID: UUID): List<DirectExecutionDTOResponse> {
        val teamsId = getTeamsIdsByUser(operatorUUID)
        if (teamsId.isEmpty()) return emptyList()

        val directExecutions = getDirectExecutionsByTeam(teamsId)
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

    private fun getTeamsIdsByUser(uuid: UUID): List<Long> {
        val teams = JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT id_team FROM team
                WHERE driver_id = :uuid OR electrician_id = :uuid
            """.trimIndent(),
            mapOf("uuid" to uuid)
        )

        return teams.map { it["id_team"] as Long }
    }

    private fun getStreetsByTeams(teamsId: List<Long>): List<Map<String, Any>> {
        return JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT pms.pre_measurement_street_id, pms.street, pms.number, pms.neighborhood,
                       pms.city, pms.state, pms.prioritized, pms.created_at,
                       pms.latitude, pms.longitude, c.contract_id, c.contractor
                FROM pre_measurement_street pms
                INNER JOIN pre_measurement p ON p.pre_measurement_id = pms.pre_measurement_id
                INNER JOIN contract c ON c.contract_id = p.contract_contract_id
                WHERE pms.team_id IN (:teams_ids) AND pms.street_status = :status
            """.trimIndent(),
            mapOf("teams_ids" to teamsId, "status" to ExecutionStatus.AVAILABLE_EXECUTION)
        )
    }

    private fun getDirectExecutionsByTeam(teamsId: List<Long>): List<Map<String, Any>> {
        return JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT de.direct_execution_id, de.instructions, de.description, de.assigned_at
                FROM direct_execution de
                WHERE de.team_id IN (:teams_ids) AND de.direct_execution_status = :status
            """.trimIndent(),
            mapOf("teams_ids" to teamsId, "status" to ExecutionStatus.AVAILABLE_EXECUTION)
        )
    }

    private fun getReservesGroupedByStreet(streetIds: List<Long>): Map<Long, List<IndirectReserve>> {
        val raw = JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT mr.material_id_reservation, mr.reserved_quantity, 
                       mr.pre_measurement_street_id, m.material_name, mr.contract_item_id,
                       m.material_power, m.material_length, ms.request_unit
                FROM material_reservation mr
                INNER JOIN material_stock ms ON ms.material_id_stock = mr.truck_material_stock_id
                INNER JOIN material m ON m.id_material = ms.material_id
                WHERE mr.direct_execution_id IS NULL
                  AND mr.pre_measurement_street_id IN (:street_ids)
            """.trimIndent(),
            mapOf("street_ids" to streetIds)
        )

        return raw.groupBy { it["pre_measurement_street_id"] as Long }
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

                    IndirectReserve(
                        reserveId = r["material_id_reservation"] as Long,
                        contractId = 0,
                        contractItemId = r["contract_item_id"] as Long,
                        materialName = name,
                        materialQuantity = BigDecimal(r["reserved_quantity"].toString()),
                        streetId = r["pre_measurement_street_id"] as Long,
                        requestUnit = r["request_unit"] as? String ?: "UN"
                    )
                }
            }
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
                WHERE mr.pre_measurement_street_id IS NULL
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


}