package com.lumos.lumosspring.execution.controller.dto;

public record PreMeasurementDTO(
        long measurementId,
        double latitude,
        double longitude,
        String number,
        String address,
        String city,
        long depositId,
        String deviceId) {
}
