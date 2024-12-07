package com.lumos.lumosspring.stock.controller.dto;

public record StockMovementResponse(
        Long id,
        String description,
        String materialName,
        int inputQuantity,
        String buyUnit,
        int quantityPackage,
        String pricePerItem,
        String supplierName,
        String company,
        String deposit,
        String employee
) { }
