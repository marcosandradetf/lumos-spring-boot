package com.lumos.lumosspring.execution.controller.dto.response;

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
