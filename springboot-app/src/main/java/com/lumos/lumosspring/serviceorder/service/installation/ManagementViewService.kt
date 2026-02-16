package com.lumos.lumosspring.serviceorder.service.installation

import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryItem
import com.lumos.lumosspring.installation.repository.view.InstallationViewRepository
import com.lumos.lumosspring.premeasurement.repository.PreMeasurementStreetItemRepository
import com.lumos.lumosspring.serviceorder.dto.installation.ReserveDTOResponse
import com.lumos.lumosspring.serviceorder.repository.installation.ReservationManagementRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.ReservationStatus
import com.lumos.lumosspring.util.Utils
import com.lumos.lumosspring.util.Utils.getCurrentTenantId
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class ManagementViewService(
    private val reservationManagementRepository: ReservationManagementRepository,
    private val preMeasurementStreetItemRepository: PreMeasurementStreetItemRepository,
    private val directExecutionRepositoryItem: DirectExecutionRepositoryItem,
    private val installationViewRepository: InstallationViewRepository
) {
    fun getPendingReservesForStockist(): ResponseEntity<Any> {
        val userId = Utils.getCurrentUserId()
        val response = ArrayList<ReserveDTOResponse?>()
        val pendingManagement =
            reservationManagementRepository.findAllByStatusAndStockistId(ReservationStatus.PENDING, userId)

        for (pRow in pendingManagement) {
            val description = pRow.description
            val installations = reservationManagementRepository.getInstallations(pRow.reservationManagementId!!)

            for (rs in installations) {
                val preMeasurementID = rs.preMeasurementId
                val directExecutionID = rs.directExecutionId

                if (preMeasurementID != null) {
                    // Consulta para itens de pré-medição
                    val items = preMeasurementStreetItemRepository.getItemsByPreMeasurementId(
                            preMeasurementID,
                            ReservationStatus.PENDING
                        )

                    response.add(
                        ReserveDTOResponse(
                            preMeasurementID,
                            null,
                            description,
                            rs.comment,
                            rs.completedName,
                            rs.teamId,
                            rs.teamName,
                            rs.notificationCode,
                            rs.depositName,
                            rs.reservationManagementId,
                            items
                        )
                    )
                } else if (directExecutionID != null) {
                    // Consulta para itens de execução direta
                    val items = directExecutionRepositoryItem.getItemsByDirectExecutionId(
                            directExecutionID,
                            ReservationStatus.PENDING
                        )

                    response.add(
                        ReserveDTOResponse(
                            null,
                            directExecutionID,
                            description,
                            rs.comment,
                            rs.completedName,
                            rs.teamId,
                            rs.teamName,
                            rs.notificationCode,
                            rs.depositName,
                            rs.reservationManagementId,
                            items
                        )
                    )
                }
            }
        }

        return ResponseEntity.ok(response)
    }

    fun getExecutions(status: String, contractId: Long?): ResponseEntity<Any> {
        when(status) {
            ExecutionStatus.WAITING_STOCKIST, ExecutionStatus.WAITING_COLLECT, ExecutionStatus.AVAILABLE_EXECUTION -> {
                return ResponseEntity.ok().body(
                    reservationManagementRepository.getServiceOrders(
                        contractId,
                        getCurrentTenantId(),
                        status
                    )
                )
            }
            else -> {
                return ResponseEntity.ok().body(
                    installationViewRepository.findInstallationsByStatus(status, getCurrentTenantId())
                )
            }
        }
    }
}