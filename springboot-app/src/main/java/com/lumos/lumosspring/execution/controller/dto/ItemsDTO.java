package com.lumos.lumosspring.execution.controller.dto;

public record ItemsDTO(long itemId, long materialId, String materialName, float itemQuantity,
                       String itemValue, long contractId) {
}
