package com.lumos.lumosspring.pre_measurement.dto.response;

import java.util.List;

public record PreMeasurementStreetResponseDTO(
        int number,
        long preMeasurementStreetId,
        String lastPower,
        Double latitude,
        Double longitude,
        String street,
        String hood,
        String city,
        String status,
        String createdBy,
        String createdAt,
        List<PreMeasurementStreetItemResponseDTO> items
) {
}
