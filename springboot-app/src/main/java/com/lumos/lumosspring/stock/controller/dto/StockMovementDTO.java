package com.lumos.lumosspring.stock.controller.dto;

import java.math.BigDecimal;

public record StockMovementDTO(String description,
                               Long materialId,
                               double inputQuantity,
                               String buyUnit,
                               String requestUnit,
                               double quantityPackage,
                               String priceTotal,
                               String supplierId,
                               double totalQuantity) {
}
