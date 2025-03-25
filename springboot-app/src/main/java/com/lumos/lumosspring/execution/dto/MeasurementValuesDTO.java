package com.lumos.lumosspring.execution.dto;


public record MeasurementValuesDTO(
        String description, int quantity, String price, String priceTotal
) {
}
