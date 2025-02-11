package com.lumos.lumosspring.stock.controller.dto;

public record StockMovementResponse(
        Long id,
        String description,
        String materialName,
        double inputQuantity,
        String buyUnit,
        String requestUnit,
        String pricePerItem,
        String supplierName,
        String company,
        String deposit,
        String employee
) { }
