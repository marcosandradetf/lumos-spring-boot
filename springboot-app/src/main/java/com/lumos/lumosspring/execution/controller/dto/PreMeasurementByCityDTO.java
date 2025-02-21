package com.lumos.lumosspring.execution.controller.dto;

import java.util.List;
import java.util.Map;

public record PreMeasurementByCityDTO(
        Map<String, List<StreetWithItemsDTO>> cities
)
{
}
