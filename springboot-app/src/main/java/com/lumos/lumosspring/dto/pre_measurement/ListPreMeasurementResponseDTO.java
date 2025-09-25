package com.lumos.lumosspring.dto.pre_measurement;

public record ListPreMeasurementResponseDTO(
        long preMeasurementId,
        long contractId,
        String city,
        String preMeasurementType,
        Integer step,
        String completeName,
        String createdAt,
        int streetsSize,
        int itemsSize
) {
}
