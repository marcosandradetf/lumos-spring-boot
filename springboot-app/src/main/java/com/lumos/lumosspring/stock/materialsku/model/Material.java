package com.lumos.lumosspring.stock.materialsku.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table
public class Material {
    @Id
    private Long idMaterial;

    private String materialName;

    private String nameForImport;

    private String materialBrand;

    private String materialPower;

    private String materialAmps;

    private String materialLength;

    private Long idMaterialType;

    private Boolean inactive;

    private String BuyUnit;

    private String RequestUnit;

    private BigDecimal conversionFactor; // utilized for calculate quantity for units than cx, rolo, etc

    public Material() {}

    public Long getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(Long idMaterial) {
        this.idMaterial = idMaterial;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getNameForImport() {
        return nameForImport;
    }

    public void setNameForImport(String nameForImport) {
        this.nameForImport = nameForImport;
    }

    public String getMaterialBrand() {
        return materialBrand;
    }

    public void setMaterialBrand(String materialBrand) {
        this.materialBrand = materialBrand;
    }

    public String getMaterialPower() {
        return materialPower;
    }

    public void setMaterialPower(String materialPower) {
        this.materialPower = materialPower;
    }

    public String getMaterialAmps() {
        return materialAmps;
    }

    public void setMaterialAmps(String materialAmps) {
        this.materialAmps = materialAmps;
    }

    public String getMaterialLength() {
        return materialLength;
    }

    public void setMaterialLength(String materialLength) {
        this.materialLength = materialLength;
    }

    public Long getIdMaterialType() {
        return idMaterialType;
    }

    public void setIdMaterialType(Long idMaterialType) {
        this.idMaterialType = idMaterialType;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }


    public String getBuyUnit() {
        return BuyUnit;
    }

    public void setBuyUnit(String buyUnit) {
        BuyUnit = buyUnit;
    }

    public String getRequestUnit() {
        return RequestUnit;
    }

    public void setRequestUnit(String requestUnit) {
        RequestUnit = requestUnit;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(BigDecimal conversionFactor) {
        this.conversionFactor = conversionFactor;
    }
}

