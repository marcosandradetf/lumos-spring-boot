package com.lumos.lumosspring.pre_measurement.dto.response;

import java.util.List;

public record PreMeasurementResponseDTO(
        long preMeasurementId,
        long contractId,
        String city,
        String depositName,
        String preMeasurementType,
        String preMeasurementStyle,
        String teamName,
        String totalPrice,
        String status,
        List<PreMeasurementStreetResponseDTO> streets
) {
}
