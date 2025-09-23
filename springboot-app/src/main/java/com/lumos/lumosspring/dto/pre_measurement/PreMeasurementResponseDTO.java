package com.lumos.lumosspring.dto.pre_measurement;

import java.util.List;

public record PreMeasurementResponseDTO(
        long preMeasurementId,
        long contractId,
        String city,
        String preMeasurementType,
        String totalPrice,
        String status,
        Integer step,
        String completeName,
        String createdAt,
        List<PreMeasurementStreetResponseDTO> streets
) {
}
