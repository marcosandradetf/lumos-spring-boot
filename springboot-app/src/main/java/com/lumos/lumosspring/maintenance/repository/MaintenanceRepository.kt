package com.lumos.lumosspring.maintenance.repository

import com.lumos.lumosspring.maintenance.model.Maintenance
import com.lumos.lumosspring.maintenance.model.MaintenanceExecutor
import com.lumos.lumosspring.maintenance.model.MaintenanceStreet
import com.lumos.lumosspring.maintenance.model.MaintenanceStreetItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface MaintenanceRepository : CrudRepository<Maintenance, UUID> {
    @Query(
        """
            SELECT
              json_agg(
                json_build_object(
                  'maintenance_id', m.maintenance_id,
                  'streets', (
                    SELECT json_agg(DISTINCT ms.maintenance_street_id)
                    FROM maintenance_street ms
                    WHERE ms.maintenance_id = m.maintenance_id
                  ),
                  'date_of_visit', m.date_of_visit,
                  'team', execs.executors
                )
                ORDER BY m.maintenance_id
              ) AS maintenances

            FROM maintenance m
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
                    FROM maintenance_executor me
                    JOIN app_user au ON au.user_id = me.user_id
                    JOIN user_role ur ON ur.id_user = au.user_id
                    JOIN role r ON r.role_id = ur.id_role
                    WHERE me.maintenance_id = m.maintenance_id
                    ORDER BY au.user_id, r.role_name
                ) t
            ) execs ON TRUE
            WHERE m.status = 'FINISHED'
                AND m.contract_id = :contractId
                AND m.finished_at >= :startDate 
                AND m.finished_at < (:endDate + INTERVAL '1 day')
            GROUP BY m.finished_at
            ORDER BY m.finished_at desc
        """
    )
    fun getReportByContractId(contractId: Long, startDate: Instant, endDate: Instant): List<Map<String, Any>>



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
