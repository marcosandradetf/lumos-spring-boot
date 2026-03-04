package com.lumos.lumosspring.installation.repository.view

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import com.lumos.lumosspring.installation.view.InstallationView
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@Repository
interface InstallationViewRepository : CrudRepository<InstallationView, Long> {
    fun findInstallationViewByReservationManagementId(reservationManagementId: Long): Optional<InstallationView>

    @Query(
        """
        select 
            iv.installation_id,
            iv.installation_type,
            iv.step,
            c.contractor,
            iv.started_at,
            iv.finished_at,
            t.team_name,
            coalesce(st.streets, '[]')::text as streets,
            coalesce(ex.executors, '[]')::text as executors
        from installation_view iv
        join contract c 
            on c.contract_id = iv.contract_id
        join team t
            on t.id_team = iv.team_id
        left join lateral (
            select jsonb_agg(
                jsonb_build_object(
                    'installation_street_id', isv.installation_street_id,
                    'installation_type', isv.installation_type,
                    'address', isv.address,
                    'latitude', isv.latitude,
                    'longitude', isv.longitude,
                    'street_status', isv.street_status,
                    'finished_at', isv.finished_at
                )
                order by isv.installation_street_id
            ) as streets
            from installation_street_view isv
            where isv.installation_id = iv.installation_id
                and isv.installation_type = iv.installation_type
                and isv.street_status = 'FINISHED'
        ) st on true
        left join lateral (
            select jsonb_agg(obj order by name) as executors
            from (
                select distinct
                    u.name || ' ' || u.last_name as name,
                    jsonb_build_object(
                        'name', u.name || ' ' || u.last_name
                    ) as obj
                from installation_executor_view iev
                join app_user u
                    on u.user_id = iev.user_id
                where iev.installation_id = iv.installation_id
                    and iev.installation_type = iv.installation_type
            ) x
        ) ex on true
        where iv.status = :status 
            and iv.tenant_id = :tenantId
    """
    )
    fun findInstallationsByStatus(status: String, tenantId: UUID): List<InstallationProjection>
    data class InstallationProjection(
        @field:JsonProperty("installation_id")
        val installationId: Long,
        @field:JsonProperty("installation_type")
        val installationType: String,
        val step: Int,
        val contractor: String,
        @field:JsonProperty("started_at")
        val startedAt: Instant,
        @field:JsonProperty("finished_at")
        val finishedAt: Instant,
        @JsonRawValue
        val streets: String,
        @field:JsonProperty("team_name")
        val teamName: String,
        @JsonRawValue
        val team: String?,
    )

    @Query(
        """
        select
            t.type_name,
            m.material_name,
            m.material_brand,
            sum(isiv.executed_quantity) as quantity,
            c.contractor,
            iv.status
        from installation_street_item_view isiv
        join material_stock ms on ms.material_id_stock = isiv.material_stock_id
        join material m on m.id_material = ms.material_id
        join material_type t on t.id_type = m.id_material_type
        join installation_street_view isv on isv.installation_street_id = isiv.installation_street_id
            and isv.installation_type = isiv.installation_type
        join installation_view iv on iv.installation_id = isv.installation_id
            and iv.installation_type = isv.installation_type
        join contract c on c.contract_id = iv.contract_id
        where iv.tenant_id = :tenantId
            and iv.started_at >= :startedAt
            and iv.started_at <= :finishedAt
            and iv.contract_id in (:contractIds)
            AND (:brands::text[] IS NULL OR m.material_brand = ANY(:brands))
            AND (:types::bigint[] IS NULL OR m.id_material_type = ANY(:types))
        group by type_name, material_name, material_brand, contractor, iv.status
    """
    )
    fun findFinishedMaterials(
        tenantId: UUID,
        startedAt: OffsetDateTime,
        finishedAt: OffsetDateTime,
        contractIds: List<Long>,
        brands: List<String>?,
        types: List<Long>?,
    ): List<ExecutedMaterials>
    data class ExecutedMaterials(
        val typeName: String,
        val materialName: String,
        val materialBrand: String?,
        val quantity: BigDecimal,
        val contractor: String,
        val status: String,
    )

}