package com.lumos.lumosspring.pre_measurement.dto.response;

import java.util.List;

public record PreMeasurementStreetResponseDTO(
        int number,
        long preMeasurementStreetId,
        String lastPower,
        Double latitude,
        Double longitude,
        String address,
        String status,
        List<PreMeasurementStreetItemResponseDTO> items
) {
}
