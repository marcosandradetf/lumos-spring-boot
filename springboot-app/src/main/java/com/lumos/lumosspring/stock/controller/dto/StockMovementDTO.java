package com.lumos.lumosspring.stock.controller.dto;

import java.math.BigDecimal;

public record StockMovementDTO(String description,
                               Long materialId,
                               int inputQuantity,
                               String buyUnit,
                               int quantityPackage,
                               String pricePerItem,
                               String supplierId) {
}
