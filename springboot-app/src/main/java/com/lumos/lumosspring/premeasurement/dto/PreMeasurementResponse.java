package com.lumos.lumosspring.premeasurement.dto;

import java.util.List;

public record PreMeasurementResponse(
        long preMeasurementId,
        long contractId,
        String city,
        String preMeasurementType,
        String totalPrice,
        String status,
        Integer step,
        String completeName,
        String createdAt,
        List<PreMeasurementStreetResponse> streets
) {
}
