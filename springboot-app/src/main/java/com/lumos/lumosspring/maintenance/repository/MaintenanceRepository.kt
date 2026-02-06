package com.lumos.lumosspring.maintenance.repository

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
            (:executionId IS NOT NULL AND maintenance_id = :executionId)
            OR (:executionId IS NULL AND contract_id = :contractId 
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
