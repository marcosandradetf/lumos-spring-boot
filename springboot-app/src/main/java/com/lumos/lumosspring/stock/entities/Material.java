package com.lumos.lumosspring.stock.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

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

    @Column("contract_reference_item_contract_reference_item_id")
    private Long contractReferenceItemId;

    public Material(Long idMaterial, String materialName, String nameForImport, String materialBrand, String materialPower, String materialAmps, String materialLength, Long idMaterialType, Boolean inactive, Long contractReferenceItemId) {
        this.idMaterial = idMaterial;
        this.materialName = materialName;
        this.nameForImport = nameForImport;
        this.materialBrand = materialBrand;
        this.materialPower = materialPower;
        this.materialAmps = materialAmps;
        this.materialLength = materialLength;
        this.idMaterialType = idMaterialType;
        this.inactive = inactive;
        this.contractReferenceItemId = contractReferenceItemId;
    }

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

    public Long getContractReferenceItemId() {
        return contractReferenceItemId;
    }

    public void setContractReferenceItemId(Long contractReferenceItemId) {
        this.contractReferenceItemId = contractReferenceItemId;
    }
}

