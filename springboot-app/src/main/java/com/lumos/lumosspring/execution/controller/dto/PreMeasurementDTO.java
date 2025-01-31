package com.lumos.lumosspring.execution.controller.dto;

public record PreMeasurementDTO(
        long measurementId,
        double latitude,
        double longitude,
        String address,
        long depositId,
        String deviceId) {
}
