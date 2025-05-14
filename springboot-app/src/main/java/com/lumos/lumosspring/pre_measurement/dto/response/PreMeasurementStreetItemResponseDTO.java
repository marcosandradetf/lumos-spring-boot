package com.lumos.lumosspring.pre_measurement.dto.response;

public record PreMeasurementStreetItemResponseDTO(
        long preMeasurementStreetItemId,
        long contractItemId,
        String contractItemName,
        String contractItemType,
        String contractItemPower,
        String contractItemLength,
        double contractItemQuantity,
        String itemStatus
) {
}
