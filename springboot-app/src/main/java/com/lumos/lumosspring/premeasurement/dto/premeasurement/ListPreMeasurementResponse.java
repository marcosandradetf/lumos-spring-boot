package com.lumos.lumosspring.premeasurement.dto.premeasurement;

public record ListPreMeasurementResponse(
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
