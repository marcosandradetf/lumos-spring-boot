package com.lumos.lumosspring.premeasurement.dto.premeasurement;

import java.util.List;

public record PreMeasurementStreetResponse(
        int number,
        long preMeasurementStreetId,
        String lastPower,
        Double latitude,
        Double longitude,
        String address,
        String status,
        List<PreMeasurementStreetItemResponse> items
) {
}
