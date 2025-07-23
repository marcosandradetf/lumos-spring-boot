package com.lumos.lumosspring.stock.entities;

import com.lumos.lumosspring.user.AppUser;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Table
public class StockMovement {
    @Id
    private Long stockMovementId;

    private String stockMovementDescription;

    private Long materialStockId;

    private Instant stockMovementRefresh;

    @Column( "user_created_id_user")
    private UUID appUserCreatedId;

    @Column("user_finished_id_user")
    private UUID appUserFinishedId;

    private double inputQuantity;

    private double totalQuantity;

    private String buyUnit;

    private String requestUnit;

    private double quantityPackage;

    private BigDecimal pricePerItem;

    private BigDecimal priceTotal;

    private Long supplierId;

    private String status;

    public StockMovement() {}

    public StockMovement(Long stockMovementId, String stockMovementDescription, Long materialStockId, Instant stockMovementRefresh, UUID appUserCreatedId, UUID appUserFinishedId, double inputQuantity, double totalQuantity, String buyUnit, String requestUnit, double quantityPackage, BigDecimal pricePerItem, BigDecimal priceTotal, Long supplierId, String status) {
        this.stockMovementId = stockMovementId;
        this.stockMovementDescription = stockMovementDescription;
        this.materialStockId = materialStockId;
        this.stockMovementRefresh = stockMovementRefresh;
        this.appUserCreatedId = appUserCreatedId;
        this.appUserFinishedId = appUserFinishedId;
        this.inputQuantity = inputQuantity;
        this.totalQuantity = totalQuantity;
        this.buyUnit = buyUnit;
        this.requestUnit = requestUnit;
        this.quantityPackage = quantityPackage;
        this.pricePerItem = pricePerItem;
        this.priceTotal = priceTotal;
        this.supplierId = supplierId;
        this.status = status;
    }

    public Long getStockMovementId() {
        return stockMovementId;
    }

    public void setStockMovementId(Long stockMovementId) {
        this.stockMovementId = stockMovementId;
    }

    public String getStockMovementDescription() {
        return stockMovementDescription;
    }

    public void setStockMovementDescription(String stockMovementDescription) {
        this.stockMovementDescription = stockMovementDescription;
    }

    public Long getMaterialStockId() {
        return materialStockId;
    }

    public void setMaterialStockId(Long materialStockId) {
        this.materialStockId = materialStockId;
    }

    public Instant getStockMovementRefresh() {
        return stockMovementRefresh;
    }

    public void setStockMovementRefresh(Instant stockMovementRefresh) {
        this.stockMovementRefresh = stockMovementRefresh;
    }

    public UUID getAppUserCreatedId() {
        return appUserCreatedId;
    }

    public void setAppUserCreatedId(UUID appUserCreatedId) {
        this.appUserCreatedId = appUserCreatedId;
    }

    public UUID getAppUserFinishedId() {
        return appUserFinishedId;
    }

    public void setAppUserFinishedId(UUID appUserFinishedId) {
        this.appUserFinishedId = appUserFinishedId;
    }

    public double getInputQuantity() {
        return inputQuantity;
    }

    public void setInputQuantity(double inputQuantity) {
        this.inputQuantity = inputQuantity;
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(double totalQuantity) {
        this.totalQuantity = totalQuantity;
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

    public double getQuantityPackage() {
        return quantityPackage;
    }

    public void setQuantityPackage(double quantityPackage) {
        this.quantityPackage = quantityPackage;
    }

    public BigDecimal getPricePerItem() {
        return pricePerItem;
    }

    public void setPricePerItem(BigDecimal pricePerItem) {
        this.pricePerItem = pricePerItem;
    }

    public BigDecimal getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(BigDecimal priceTotal) {
        this.priceTotal = priceTotal;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}