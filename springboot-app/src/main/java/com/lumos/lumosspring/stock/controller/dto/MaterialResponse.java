package com.lumos.lumosspring.stock.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lumos.lumosspring.stock.entities.Material;

@JsonInclude(JsonInclude.Include.NON_NULL)  // Isso vai garantir que valores nulos não sejam serializados
public record MaterialResponse(long idMaterial, String materialName, String materialBrand, String materialPower, String materialAmps, String materialLength,
                               String buyUnit, String requestUnit, Integer stockQt, Boolean inactive, String materialType, String materialGroup,
                               String deposit, String company) {
    public MaterialResponse(Material material) {
        this(
                material.getIdMaterial(),
                material.getMaterialName(),
                material.getMaterialBrand(),
                material.getMaterialPower(),
                material.getMaterialAmps(),
                material.getMaterialLength(),
                material.getBuyUnit(),
                material.getRequestUnit(),
                material.getStockQuantity(),
                material.isInactive(),
                material.getMaterialType() != null ? material.getMaterialType().getTypeName() : null,
                material.getMaterialType() != null ? material.getMaterialType().getGroup().getGroupName(): null,
                material.getDeposit() != null ? material.getDeposit().getDepositName() : null,
                material.getCompany() != null ? material.getCompany().getCompanyName() : null
        );
    }
}
