package com.lumos.lumosspring.stock.order.installationrequest.dto

import com.lumos.lumosspring.contract.dto.ItemResponseDTO

data class ReserveDTOResponse(
    val preMeasurementId: Long?,
    val directExecutionId: Long?,
    val description: String,
    val comment: String?,
    val assignedBy: String,
    val teamId: Long,
    val teamName: String,
    val truckDepositName: String,
    val items: List<ItemResponseDTO>,
)