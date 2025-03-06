package com.lumos.lumosspring.execution.controller.dto.response;

import java.time.Instant;
import java.util.List;

public record PreMeasurementResponseDTO(
        long preMeasurementId,
        String city,
        String createdBy,
        String createdAt,
        String depositName,
        String preMeasurementType,
        String preMeasurementStyle,
        String teamName,
        String totalPrice,
        List<PreMeasurementStreetResponseDTO> streets
) {
}
