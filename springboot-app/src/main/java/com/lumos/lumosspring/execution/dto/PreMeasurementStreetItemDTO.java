package com.lumos.lumosspring.execution.dto;

public record PreMeasurementStreetItemDTO(Long itemId,
                                          String materialId,
                                          int materialQuantity,
                                          Long measurementId,
                                          String material) {
}
