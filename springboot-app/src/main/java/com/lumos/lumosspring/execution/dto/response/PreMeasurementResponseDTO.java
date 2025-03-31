package com.lumos.lumosspring.execution.dto.response;

import java.util.List;

public record PreMeasurementResponseDTO(
        long preMeasurementId,
        long contractId,
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
