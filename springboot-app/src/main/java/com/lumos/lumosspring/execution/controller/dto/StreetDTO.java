package com.lumos.lumosspring.execution.controller.dto;

import java.util.List;

public record StreetDTO(long streetId, String name, long preMeasurementId,
                        List<ItemsDTO> items) {
}
