package com.lumos.lumosspring.execution.controller.dto;

public record ItemsDTO(long itemId, long materialId, String itemName, float itemQuantity,
                       String itemValue, long contractId) {
}
