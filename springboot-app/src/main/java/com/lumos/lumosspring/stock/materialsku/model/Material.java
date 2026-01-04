package com.lumos.lumosspring.stock.materialsku.model;

import com.lumos.lumosspring.authentication.model.TenantEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table
public class Material extends TenantEntity {
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
    private Long parentMaterialId;
    private Long subtypeId;
    private String materialFunction;
    private String materialModel;
    private String materialWidth;
    private String materialGauge;
    private String materialWeight;
    private String barcode;

    public Material() {
    }

    public Material(
            Long parentMaterialId,
            String materialName,
            Long materialType,
            Long materialSubtype,
            String materialFunction,
            String materialModel,
            String materialBrand,
            String materialAmps,
            String materialLength,
            String materialWidth,
            String materialPower,
            String materialGauge,
            String materialWeight,
            String barcode,
            String buyUnit,
            String requestUnit
    ) {
        this.parentMaterialId = parentMaterialId;
        this.materialName = materialName;
        this.idMaterialType = materialType;
        this.subtypeId = materialSubtype;
        this.materialFunction = materialFunction;
        this.materialModel = materialModel;
        this.materialBrand = materialBrand;
        this.materialAmps = materialAmps;
        this.materialLength = materialLength;
        this.materialWidth = materialWidth;
        this.materialPower = materialPower;
        this.materialGauge = materialGauge;
        this.materialWeight = materialWeight;
        this.barcode = barcode;
        this.BuyUnit = buyUnit;
        this.RequestUnit = requestUnit;
    }

    public Material(
            String materialName,
            Long materialType,
            Long materialSubtype
    ) {
        this.materialName = materialName;
        this.idMaterialType = materialType;
        this.subtypeId = materialSubtype;
    }

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


    public Long getParentMaterialId() {
        return parentMaterialId;
    }

    public void setParentMaterialId(Long parentMaterialId) {
        this.parentMaterialId = parentMaterialId;
    }

    public Long getSubtypeId() {
        return subtypeId;
    }

    public void setSubtypeId(Long subtypeId) {
        this.subtypeId = subtypeId;
    }

    public String getMaterialFunction() {
        return materialFunction;
    }

    public void setMaterialFunction(String materialFunction) {
        this.materialFunction = materialFunction;
    }

    public String getMaterialModel() {
        return materialModel;
    }

    public void setMaterialModel(String materialModel) {
        this.materialModel = materialModel;
    }

    public String getMaterialWidth() {
        return materialWidth;
    }

    public void setMaterialWidth(String materialWidth) {
        this.materialWidth = materialWidth;
    }

    public String getMaterialGauge() {
        return materialGauge;
    }

    public void setMaterialGauge(String materialGauge) {
        this.materialGauge = materialGauge;
    }

    public String getMaterialWeight() {
        return materialWeight;
    }

    public void setMaterialWeight(String materialWeight) {
        this.materialWeight = materialWeight;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void update(
            Long parentMaterialId,
            String materialName,
            Long materialType,
            Long materialSubtype,
            String materialFunction,
            String materialModel,
            String materialBrand,
            String materialAmps,
            String materialLength,
            String materialWidth,
            String materialPower,
            String materialGauge,
            String materialWeight,
            String barcode,
            Boolean inactive,
            String buyUnit,
            String requestUnit
    ) {
        this.parentMaterialId = parentMaterialId;
        this.materialName = materialName;
        this.idMaterialType = materialType;
        this.subtypeId = materialSubtype;
        this.materialFunction = materialFunction;
        this.materialModel = materialModel;
        this.materialBrand = materialBrand;
        this.materialAmps = materialAmps;
        this.materialLength = materialLength;
        this.materialWidth = materialWidth;
        this.materialPower = materialPower;
        this.materialGauge = materialGauge;
        this.materialWeight = materialWeight;
        this.barcode = barcode;
        this.inactive = inactive;
        this.BuyUnit = buyUnit;
        this.RequestUnit = requestUnit;
    }
}

