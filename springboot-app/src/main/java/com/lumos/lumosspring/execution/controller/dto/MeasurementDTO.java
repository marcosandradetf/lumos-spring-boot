package com.lumos.lumosspring.execution.controller.dto;

import java.util.List;

public record MeasurementDTO(
        PreMeasurementDTO measurement,
        List<ItemsDTO> items
) {
}
