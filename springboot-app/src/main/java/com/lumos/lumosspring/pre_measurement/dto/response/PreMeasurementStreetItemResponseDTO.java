package com.lumos.lumosspring.pre_measurement.dto.response;

public record PreMeasurementStreetItemResponseDTO(
        long preMeasurementStreetItemId,
        long contractItemId,
        String contractReferenceItemName,
        String contractReferenceItemType,
        String contractReferenceItemPower,
        String contractReferenceItemLength,
        double measuredQuantity,
        String itemStatus
) {
}
