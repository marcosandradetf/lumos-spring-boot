package com.lumos.lumosspring.dashboard.repository.map

import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
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
        val teamId: Long,
        val teamName: String,
        val lat: Double,
        val lng: Double,
        val address: String
    )
    fun getExecutions(tenantId: UUID): List<GeoExecution> {
        val sql =
            """
            select
                iv.installation_id::text as id,
                iv.description as title,
                'INSTALLATION' as type,
                iv.status as status,
                iv.team_id,
                t.team_name,
                isv.latitude as lat,
                isv.longitude as lng,
                isv.address
            from installation_view iv
            join installation_street_view isv on isv.installation_id = iv.installation_id
                and iv.installation_type = isv.installation_type
                and isv.latitude is not null
                and isv.longitude is not null
            join team t on t.id_team = iv.team_id
            where iv.status in ('IN_PROGRESS', 'FINISHED')
                and iv.tenant_id = :tenantId
            UNION ALL
            select
                m.maintenance_id::text,
                c.contractor,
                'MAINTENANCE',
                case 
                    when m.status = 'DRAFT' then 'IN_PROGRESS'
                    else m.status
                end as status,
                t.team_id,
                t.team_name,
                ms.latitude,
                ms.longitude,
                ms.address
            from maintenance m
            join maintenance_street ms on ms.maintenance_id = m.maintenance_id
                and ms.latitude is not null
                and ms.longitude is not null
            join contract c on c.contract_id = m.contract_id
            CROSS JOIN LATERAL (
                select t.id_team as team_id, t.team_name
                from app_user u
                join maintenance_executor me on me.user_id = u.user_id
                join team t on t.id_team = u.team_id
                where me.maintenance_id = m.maintenance_id
                limit 1
            ) t
            where m.status in ('DRAFT', 'FINISHED')
                and m.tenant_id = :tenantId
        """
        val params = mapOf("tenantId" to tenantId)
        return jdbcTemplate.query(sql, params, DataClassRowMapper(GeoExecution::class.java))
    }
}
