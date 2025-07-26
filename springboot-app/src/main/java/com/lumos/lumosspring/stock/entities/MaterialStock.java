package com.lumos.lumosspring.stock.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table
public class MaterialStock {

    @Id
    private Long materialIdStock;

    private Long materialId;

    private Long depositId;

    private Long companyId;

    private String buyUnit;

    private String requestUnit;

    private BigDecimal stockQuantity;

    private BigDecimal stockAvailable;

    private BigDecimal costPerItem;

    private BigDecimal costPrice;

    private boolean inactive;

    public  MaterialStock() {}

    public MaterialStock(Long materialIdStock, Long materialId, Long depositId, Long companyId, String buyUnit, String requestUnit, BigDecimal stockQuantity, BigDecimal stockAvailable, BigDecimal costPerItem, BigDecimal costPrice, boolean inactive) {
        this.materialIdStock = materialIdStock;
        this.materialId = materialId;
        this.depositId = depositId;
        this.companyId = companyId;
        this.buyUnit = buyUnit;
        this.requestUnit = requestUnit;
        this.stockQuantity = stockQuantity;
        this.stockAvailable = stockAvailable;
        this.costPerItem = costPerItem;
        this.costPrice = costPrice;
        this.inactive = inactive;
    }

    public Long getMaterialIdStock() {
        return materialIdStock;
    }

    public void setMaterialIdStock(Long materialIdStock) {
        this.materialIdStock = materialIdStock;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getDepositId() {
        return depositId;
    }

    public void setDepositId(Long depositId) {
        this.depositId = depositId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getBuyUnit() {
        return buyUnit;
    }

    public void setBuyUnit(String buyUnit) {
        this.buyUnit = buyUnit;
    }

    public String getRequestUnit() {
        return requestUnit;
    }

    public void setRequestUnit(String requestUnit) {
        this.requestUnit = requestUnit;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(BigDecimal stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public BigDecimal getCostPerItem() {
        return costPerItem;
    }

    public void setCostPerItem(BigDecimal costPerItem) {
        this.costPerItem = costPerItem;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public void addStockQuantity(BigDecimal quantityCompleted) {
        this.stockQuantity = this.stockQuantity.add(quantityCompleted);
    }

    public void addStockAvailable(BigDecimal quantityCompleted) {
        this.stockQuantity = this.stockAvailable.add(quantityCompleted);
    }
}
