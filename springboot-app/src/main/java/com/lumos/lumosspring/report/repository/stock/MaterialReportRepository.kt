package com.lumos.lumosspring.report.repository.stock

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
class MaterialReportRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun findTeamReportsJson(tenantId: UUID, contractId: Long?, start: OffsetDateTime, end: OffsetDateTime): List<TeamReportJsonProjection> {
        return namedParameterJdbcTemplate.query(
            """
                    WITH records_by_installation AS (
                        SELECT
                            t.team_name,
                            c.company_logo,
                            c.fantasy_name,
                            c.company_address,
                            c.company_cnpj,
                            c.company_phone,                            
                            iv.installation_id,
                            iv.description,
                            iv.installation_type,
                            iv.status,
                            iv.contract_id,
                            jsonb_agg(
                                jsonb_build_object(
                                    'material_name', m.material_name,
                                    'released_quantity', mr.reserved_quantity,
                                    'quantity_completed', mr.quantity_completed,
                                    'balance', mr.reserved_quantity - mr.quantity_completed,
                                    'truck_quantity', truck.stock_available,
                                    'deposit_name', d.deposit_name,
                                    'created_at', mr.created_at,
                                    'creator', creator.name || ' ' || creator.last_name,
                                    'collected_at', mr.collected_at,
                                    'responsible', responsible.name || ' ' || responsible.last_name
                                ) ORDER BY mr.created_at
                            ) AS records
                        FROM material_reservation mr
                        JOIN app_user creator ON creator.user_id = created_by
                        LEFT JOIN app_user responsible ON responsible.user_id = released_by
                        JOIN material_stock truck ON truck.material_id_stock = mr.truck_material_stock_id
                        JOIN material_stock ms ON ms.material_id_stock = mr.central_material_stock_id
                        JOIN deposit d ON d.id_deposit = ms.deposit_id
                        JOIN material m ON m.id_material = ms.material_id
                        JOIN team t ON t.id_team = mr.team_id
                        JOIN installation_view iv
                            ON iv.installation_id = coalesce(mr.direct_execution_id, mr.pre_measurement_id)
                            AND iv.installation_type =
                                CASE
                                    WHEN mr.direct_execution_id IS NOT NULL THEN 'DIRECT_EXECUTION'
                                    ELSE 'PRE_MEASUREMENT'
                                END
                        CROSS JOIN (
                            SELECT *
                            FROM company
                            WHERE tenant_id = :tenantId
                            ORDER BY 1
                            LIMIT 1
                        ) c
                        WHERE mr.central_material_stock_id IS NOT NULL
                            AND iv.status IN ('WAITING_STOCKIST', 'WAITING_RESERVE_CONFIRMATION', 'WAITING_COLLECT', 'AVAILABLE_EXECUTION', 'IN_PROGRESS', 'FINISHED')
                            AND iv.tenant_id = :tenantId
                            AND c.tenant_id = :tenantId
                            ${if(contractId != null) {
                                """
                                    iv.contract_id = :contractId
                                """.trimIndent()
                            } else {
                                """
                                    AND mr.created_at >= :start
                                    AND mr.created_at <= :end
                                """.trimIndent()
                            }}
                        GROUP BY 
                            t.team_name, 
                            iv.installation_id, 
                            iv.description, 
                            iv.installation_type, 
                            iv.status, 
                            iv.contract_id,
                            c.company_logo,
                            c.fantasy_name,
                            c.company_address,
                            c.company_cnpj,
                            c.company_phone
                    )
                    SELECT
                        team_name,
                        company_logo,
                        fantasy_name,
                        company_address,
                        company_cnpj,
                        company_phone,   
                        jsonb_agg(
                            jsonb_build_object(
                                'installation_id', installation_id,
                                'description', description,
                                'installation_type', installation_type,
                                'status', status,
                                'contract_id', contract_id,
                                'records', records
                            ) ORDER BY description
                        ) AS installations_json
                    FROM records_by_installation
                    GROUP BY 
                        team_name,
                        company_logo,
                        fantasy_name,
                        company_address,
                        company_cnpj,
                        company_phone   
                    ORDER BY team_name
            """.trimIndent(),
            mapOf(
                "tenantId" to tenantId,
                "contractId" to contractId,
                "start" to start,
                "end" to end,
            ),
            DataClassRowMapper(TeamReportJsonProjection::class.java)
        )
    }
    data class TeamReportJsonProjection(
        @field:JsonProperty("team_name")
        val teamName: String,

        @field:JsonProperty("company_logo")
        val companyLogo: String?,
        @field:JsonProperty("fantasy_name")
        val fantasyName: String,
        @field:JsonProperty("company_address")
        val companyAddress: String,
        @field:JsonProperty("company_cnpj")
        val companyCnpj: String,
        @field:JsonProperty("company_phone")
        val companyPhone: String,
        @field:JsonProperty("installations_json")
        val installationsJson: String
    )
}
