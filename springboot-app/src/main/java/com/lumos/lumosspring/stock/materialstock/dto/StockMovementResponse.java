package com.lumos.lumosspring.stock.materialstock.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        String description,
        String materialName,
        BigDecimal inputQuantity,
        String buyUnit,
        String requestUnit,
        String priceTotal,
        String pricePerItem,
        String deposit,
        String responsible,
        Instant dateOf,
        BigDecimal totalQuantity,
        BigDecimal quantityPackage
) {
}
