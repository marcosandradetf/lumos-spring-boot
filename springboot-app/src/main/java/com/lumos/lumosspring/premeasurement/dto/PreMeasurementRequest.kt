package com.lumos.lumosspring.premeasurement.dto

import java.math.BigDecimal
import java.util.*

data class PreMeasurementRequest(
    val preMeasurementId: UUID? = null,
    val contractId: Long,
    val streets: List<PreMeasurementStreetItemsRequest>,
)

data class PreMeasurementStreetItemsRequest(
    val street: PreMeasurementStreetRequest,
    val items: List<PreMeasurementItemRequest>
)


data class PreMeasurementStreetRequest(
    val preMeasurementStreetId: UUID? = null,
    val lastPower: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
)

data class PreMeasurementItemRequest(
    val preMeasurementId: UUID? = null,
    val preMeasurementStreetId: UUID? = null,
    val contractReferenceItemId: Long,
    val measuredQuantity: BigDecimal,
)
