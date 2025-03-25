package com.lumos.lumosspring.execution.dto.response;

import java.util.List;

public record PreMeasurementStreetResponseDTO(
        long preMeasurementStreetId,
        String lastPower,
        double latitude,
        double longitude,
        String address,
        List<PreMeasurementStreetItemResponseDTO> items
) {
}
