package com.lumos.lumosspring.stock.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.entities.MaterialStock;

@JsonInclude(JsonInclude.Include.NON_NULL)  // Isso vai garantir que valores nulos não sejam serializados
public record MaterialResponse(long idMaterial, String materialName, String materialBrand, String materialPower, String materialAmps, String materialLength,
                               String buyUnit, String requestUnit, double stockQt, Boolean inactive, String materialType, String materialGroup,
                               String deposit, String company) {
    public MaterialResponse(MaterialStock material) {
        this(
                material.getMaterialIdStock(),
                material.getMaterial().getMaterialName(),
                material.getMaterial().getMaterialBrand(),
                material.getMaterial().getMaterialPower(),
                material.getMaterial().getMaterialAmps(),
                material.getMaterial().getMaterialLength(),
                material.getMaterial().getBuyUnit(),
                material.getMaterial().getRequestUnit(),
                material.getMaterial().getStockQuantity(),
                material.getMaterial().isInactive(),
                material.getMaterial().getMaterialType() != null ? material.getMaterial().getMaterialType().getTypeName() : null,
                material.getMaterial().getMaterialType() != null ? material.getMaterial().getMaterialType().getGroup().getGroupName(): null,
                material.getDeposit() != null ? material.getDeposit().getDepositName() : null,
                material.getCompany() != null ? material.getCompany().getCompanyName() : null
        );
    }
}
