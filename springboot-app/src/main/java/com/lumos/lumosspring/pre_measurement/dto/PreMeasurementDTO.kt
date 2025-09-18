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
    val contractId: Long,
    var lastPower: String?,
    var latitude: Double?,
    var longitude: Double?,
    var address: String?
)

data class PreMeasurementStreetItemDTO(
    val preMeasurementItemId: UUID,
    var preMeasurementStreetId: UUID,

    val contractReferenceItemId: Long,
    val measuredQuantity: BigDecimal,

    val contractId: Long,
)
