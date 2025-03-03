package com.lumos.lumosspring.execution.controller.dto.response;

public record PreMeasurementStreetItemResponseDTO(
        long preMeasurementStreetItemId,
        long materialId,
        String materialName,
        String materialType,
        String materialPower,
        String materialLength,
        double materialQuantity) {
}
