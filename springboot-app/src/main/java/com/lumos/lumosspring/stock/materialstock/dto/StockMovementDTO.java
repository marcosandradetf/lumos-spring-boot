package com.lumos.lumosspring.stock.materialstock.dto;

import java.math.BigDecimal;

public record StockMovementDTO(
        Long materialStockId,
        String description,
        BigDecimal inputQuantity,
        BigDecimal quantityPackage,
        String priceTotal,
        BigDecimal totalQuantity) {
}