package com.lumos.lumosspring.execution.controller.dto;

import java.util.List;

public record PreMeasurementDTO(
        PreMeasurementStreetDTO measurement,
        List<PreMeasurementStreetItemDTO> items
) {
}
