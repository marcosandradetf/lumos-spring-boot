package com.lumos.lumosspring.pre_measurement.dto.response;

public record PreMeasurementStreetItemResponseDTO(
        long preMeasurementStreetItemId,
        long materialId,
        long contractItemId,
        String materialName,
        String materialType,
        String materialPower,
        String materialLength,
        double materialQuantity
) {
}
