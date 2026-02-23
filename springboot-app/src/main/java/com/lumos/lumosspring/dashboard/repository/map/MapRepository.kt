package com.lumos.lumosspring.dashboard.repository.map

import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class MapRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    data class GeoExecution (
        val id: String,
        val title: String,
        val type: String,
        val status: String,
        val lat: Double,
        val lng: Double,
        val address: String,
        val finishedAt: Instant?,
        val teamId: Long,
        val teamName: String,
        val photoUri: String?,
        val pointNumber: Int?,
    )
    fun getExecutions(tenantId: UUID): List<GeoExecution> {
        val sql =
            """
                with executions as (
                    select 
                        iv.installation_id::text as id,
                        iv.description           as title,
                        iv.installation_type     as type,
                        iv.status                as status,
                        isv.latitude             as lat,
                        isv.longitude            as lng,
                        isv.address,
                        isv.finished_at,
                        iv.team_id,
                        isv.execution_photo_uri as photo_uri,
                        isv.point_number
                from installation_view iv
                join installation_street_view isv on isv.installation_id = iv.installation_id
                    and iv.installation_type = isv.installation_type
                    and isv.latitude is not null
                    and isv.longitude is not null
                where iv.status in ('IN_PROGRESS', 'FINISHED')
                    and iv.tenant_id = :tenantId
                    and iv.started_at >= (now() - interval '1 year')
                UNION ALL
                select 
                    m.maintenance_id::text,
                    c.contractor,
                    'MAINTENANCE',
                    case
                        when m.status = 'DRAFT' then 'IN_PROGRESS'
                        else m.status
                    end as status,
                    ms.latitude,
                    ms.longitude,
                    ms.address,
                    ms.finished_at,
                    m.team_id,
                    null,
                    ms.point_number
                from maintenance m
                join maintenance_street ms on ms.maintenance_id = m.maintenance_id
                    and ms.latitude is not null
                    and ms.longitude is not null
                    and m.date_of_visit >= (now() - interval '1 year')
                join contract c on c.contract_id = m.contract_id
                where m.status in ('DRAFT', 'FINISHED')
                and m.tenant_id = :tenantId
                )
                select 
                    e.id,
                    e.title,
                    e.type,
                    e.status,
                    e.lat,
                    e.lng,
                    e.address,
                    e.finished_at,
                    e.team_id,
                    t.team_name,
                    e.photo_uri,
                    e.point_number
                from executions e
                join team t on e.team_id = t.id_team
                order by e.title
            """.trimIndent()
        val params = mapOf("tenantId" to tenantId)
        return jdbcTemplate.query(sql, params, DataClassRowMapper(GeoExecution::class.java))
    }
}
