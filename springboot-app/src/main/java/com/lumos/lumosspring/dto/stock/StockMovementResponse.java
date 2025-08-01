package com.lumos.lumosspring.dto.stock;

import java.math.BigDecimal;

public record StockMovementResponse(
        Long id,
        String description,
        String materialName,
        BigDecimal inputQuantity,
        String buyUnit,
        String requestUnit,
        String pricePerItem,
        String supplierName,
        String company,
        String deposit,
        String employee
) { }
