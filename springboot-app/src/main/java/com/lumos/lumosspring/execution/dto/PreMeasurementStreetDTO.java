package com.lumos.lumosspring.execution.dto;

public record PreMeasurementStreetDTO(
        long measurementId,
        String lastPower,
        double latitude,
        double longitude,
        String address,
        String street,
        String city,
        long depositId,
        String deviceId,
        String depositName,
        String measurementType,
        String measurementStyle,
        String createdBy) {
}
