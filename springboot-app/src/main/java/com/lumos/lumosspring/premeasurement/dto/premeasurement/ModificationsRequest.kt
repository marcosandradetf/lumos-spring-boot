package com.lumos.lumosspring.premeasurement.dto.premeasurement

data class ModificationsRequest(
    val cancelledStreets: List<CancelledStreets>,
    val cancelledItems: List<CancelledItems>,
    val changedItems: List<ChangedItems>,
)

data class CancelledStreets(
    val streetId: Long
)

data class CancelledItems(
    val streetId: Long,
    val itemId: Long,
)

data class ChangedItems(
    val streetId: Long,
    val itemId: Long,
    val quantity: Long,
    val newContractReferenceId: Long,
)

///

data class DeletePreMeasurementDTO(
    val preMeasurementId: Long,
    val preMeasurementStreetIds: List<Long>,
    val userUUID: String,
)