package com.lumos.lumosspring.pre_measurement.dto

import java.math.BigDecimal
import java.util.*

data class PreMeasurementDTO(
    val preMeasurementId: UUID,
    val contractId: Long,
    val streets: List<PreMeasurementStreetItemsDTO>,
)

data class PreMeasurementStreetItemsDTO(
    val street: PreMeasurementStreetDTO,
    val items: List<PreMeasurementStreetItemDTO>
)


data class PreMeasurementStreetDTO(
    val preMeasurementStreetId: UUID,
    val lastPower: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
)

data class PreMeasurementStreetItemDTO(
    val preMeasurementId: String,
    val preMeasurementStreetId: UUID,
    val contractReferenceItemId: Long,
    val measuredQuantity: BigDecimal,
)
