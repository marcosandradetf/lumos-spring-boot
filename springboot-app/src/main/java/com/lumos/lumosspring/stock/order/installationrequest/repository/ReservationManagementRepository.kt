package com.lumos.lumosspring.stock.order.installationrequest.repository

import com.lumos.lumosspring.stock.order.installationrequest.model.ReservationManagement
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

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