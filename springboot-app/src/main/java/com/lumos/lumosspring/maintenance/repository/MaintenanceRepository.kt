package com.lumos.lumosspring.maintenance.repository

import com.lumos.lumosspring.installation.repository.view.InstallationViewRepository.ExecutedMaterials
import com.lumos.lumosspring.maintenance.model.Maintenance
import com.lumos.lumosspring.maintenance.model.MaintenanceExecutor
import com.lumos.lumosspring.maintenance.model.MaintenanceStreet
import com.lumos.lumosspring.maintenance.model.MaintenanceStreetItem
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface MaintenanceRepository : CrudRepository<Maintenance, UUID> {
    @Modifying
    @Query("""
        UPDATE maintenance
        SET report_view_at = now()
        WHERE
        (
            (:executionId::uuid IS NOT NULL AND maintenance_id = :executionId)
            OR (:executionId::uuid IS NULL AND contract_id = :contractId 
                AND date_of_visit >= :start 
                AND date_of_visit < :end)
        )
        AND report_view_at IS NULL
    """)
    fun registerGeneration(
        @Param("contractId") contractId: Long,
        @Param("start") start: OffsetDateTime,
        @Param("end") end: OffsetDateTime,
        @Param("executionId") executionId: UUID?
    )

    @Query(
        """
            select
                t.type_name,
                m.material_name,
                m.material_brand,
                sum(isiv.quantity_executed) as quantity,
                c.contractor,
                iv.status
            from maintenance_street_item isiv
            join material_stock ms on ms.material_id_stock = isiv.material_stock_id
            join material m on m.id_material = ms.material_id
            join material_type t on t.id_type = m.id_material_type
            join maintenance iv on iv.maintenance_id = isiv.maintenance_id
            join contract c on c.contract_id = iv.contract_id
            where iv.tenant_id = :tenantId
                and iv.date_of_visit >= :startedAt
                and iv.date_of_visit <= :finishedAt
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
        types: List<Long>?
    ): List<ExecutedMaterials>

}

@Repository
interface MaintenanceStreetRepository : CrudRepository<MaintenanceStreet, UUID> {
}

@Repository
interface MaintenanceStreetItemRepository : CrudRepository<MaintenanceStreetItem, UUID> {
}

@Repository
interface MaintenanceExecutorRepository : CrudRepository<MaintenanceExecutor, UUID> {
}
