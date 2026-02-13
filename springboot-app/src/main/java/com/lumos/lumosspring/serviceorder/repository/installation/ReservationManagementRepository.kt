package com.lumos.lumosspring.serviceorder.repository.installation

import com.lumos.lumosspring.serviceorder.model.installation.ReservationManagement
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ReservationManagementRepository : CrudRepository<ReservationManagement, Long> {
    fun findAllByStatusAndStockistId(status: String, stockistId: UUID): List<ReservationManagement>

    @Query(
        """
            select
                p.pre_measurement_id,
                null as direct_execution_id,
                p.comment,
                au.name || ' ' || au.last_name as completed_name,
                p.team_id,
                t.team_name,
                t.notification_code,
                d.deposit_name
            from pre_measurement p
            inner join team t on t.id_team = p.team_id
            inner join deposit d on d.id_deposit = t.deposit_id_deposit
            inner join app_user au on au.user_id = p.assign_by_user_id
            where p.reservation_management_id = :reservationManagementId
            UNION ALL
            select
                null as pre_measurement_id,
                de.direct_execution_id,
                de.instructions as comment,
                au.name || ' ' || au.last_name as completed_name,
                de.team_id,
                t.team_name,
                t.notification_code,
                d.deposit_name
            from direct_execution de
            inner join team t on t.id_team = de.team_id
            inner join deposit d on d.id_deposit = t.deposit_id_deposit
            inner join app_user au on au.user_id = de.assigned_user_id
            where de.reservation_management_id = :reservationManagementId
        """
    )
    fun getInstallations(reservationManagementId: Long): List<InstallationView>

    @Query(
        """
            select
                iv.installation_id,
                iv.installation_type,
                rm.reservation_management_id,
                rm.description,
                u.user_id,
                u.name || ' ' || u.last_name as user_name,
                iv.status as installation_status,
                iv.available_at,
                iv.team_id,
                t.team_name
            from installation_view iv
            join reservation_management rm on iv.reservation_management_id = rm.reservation_management_id
            join app_user u on u.user_id = rm.stockist_id
            join team t on t.id_team = iv.team_id
            where 
                (:contractId is null and iv.status = :status and iv.tenant_id = :tenantId)
                OR (:contractId is not null and iv.contract_id = :contractId and iv.status = :status)
            order by iv.contract_id, iv.step
        """
    )
    fun getExecutions(contractId: Long?, tenantId: UUID, status: String): List<ExecutionDto>

    @Modifying
    @Query("update reservation_management set stockist_id = :stockistId where reservation_management_id = :reservationManagementId")
    fun updateStockistId(reservationManagementId: Long, stockistId: UUID)

    data class ExecutionDto(
        val installationId: Long,
        val installationType: String,
        val reservationManagementId: Long,
        val description: String,
        val userId: UUID,
        val userName: String,
        val installationStatus: String,
        val availableAt: Instant,
        val teamId: Long,
        val teamName: String,
    )


    data class InstallationView(
        val preMeasurementId: Long?,
        val directExecutionId: Long?,
        val comment: String?,
        val completedName: String,
        val teamId: Long,
        val teamName: String,
        val notificationCode: String,
        val depositName: String,
    )
}