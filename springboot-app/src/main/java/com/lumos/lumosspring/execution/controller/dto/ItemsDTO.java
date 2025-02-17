package com.lumos.lumosspring.execution.controller.dto;

public record ItemsDTO(Long itemId,
                       String materialId,
                       int materialQuantity,
                       String lastPower,
                       Long measurementId,
                       String material) {
}
