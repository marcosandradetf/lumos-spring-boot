package com.lumos.lumosspring.execution.controller.dto;


public record MeasurementValuesDTO(
        String description, int quantity, String price, String priceTotal
) {
}
