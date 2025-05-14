package com.lumos.lumosspring.pre_measurement.dto

data class PreMeasurementDTO(
    val contractId: Long,
    val streets: List<PreMeasurementStreetItemsDTO>,
)

data class PreMeasurementStreetItemsDTO(
    val street: PreMeasurementStreetDTO,
    val items: List<PreMeasurementStreetItemDTO>
)

data class PreMeasurementStreetDTO(
    val preMeasurementStreetId: Long,
    val contractId: Long,
    var lastPower: String?,
    var latitude: Double,
    var longitude: Double,
    var street: String,
    var number: String?,
    var neighborhood: String,
    var city: String,
    var state: String?,
)

data class PreMeasurementStreetItemDTO(
    val preMeasurementItemId: Long = 0,
    var preMeasurementStreetId: Long,
    val itemContractId: Long,
    val itemContractQuantity: Int,
    val contractId: Long,
)
