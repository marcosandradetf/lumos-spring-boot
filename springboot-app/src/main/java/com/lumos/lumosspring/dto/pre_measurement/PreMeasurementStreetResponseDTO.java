package com.lumos.lumosspring.dto.pre_measurement;

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
