package com.lumos.lumosspring.execution.controller.dto;

import java.util.List;

public record PreMeasurementDTO(long preMeasurementId, String description, String UF,
                                String city, String region, List<StreetDTO> streets) {
}
