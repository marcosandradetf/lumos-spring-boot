package com.lumos.lumosspring.execution.controller.dto;

public record PreMeasurementDTO(
        long measurementId,
        double latitude,
        double longitude,
        String address,
        String city,
        long depositId,
        String deviceId,
        String depositName,
        String measurementType,
        String measurementStyle,
        String createdBy) {
}
