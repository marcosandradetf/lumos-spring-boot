package com.lumos.lumosspring.serviceorder.dto.installation

import com.lumos.lumosspring.contract.dto.ItemResponseDTO

data class ReserveDTOResponse(
    val preMeasurementId: Long?,
    val directExecutionId: Long?,
    val description: String?,
    val comment: String?,
    val assignedBy: String,
    val teamId: Long,
    val teamName: String,
    val teamNotificationCode: String,
    val truckDepositName: String,
    val reservationManagementId: Long,
    val items: List<ItemResponseDTO>,
)