package com.lumos.lumosspring.pre_measurement.dto;


public record MeasurementValuesDTO(
        String description, int quantity, String price, String priceTotal
) {
}
