package com.lumos.lumosspring.execution.dto;

import java.util.List;

public record PreMeasurementDTO(
        PreMeasurementStreetDTO measurement,
        List<PreMeasurementStreetItemDTO> items
) {
}
