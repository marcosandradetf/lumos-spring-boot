package com.lumos.lumosspring.dto.stock;

import java.math.BigDecimal;

public record StockMovementDTO(String description,
                               Long materialId,
                               BigDecimal inputQuantity,
                               String buyUnit,
                               String requestUnit,
                               BigDecimal quantityPackage,
                               String priceTotal,
                               String supplierId,
                               BigDecimal totalQuantity) {
}
