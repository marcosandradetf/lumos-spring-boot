package com.lumos.lumosspring.stock.entities;

import com.lumos.lumosspring.user.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_movement_id")
    private Long stockMovementId;

    @Column(columnDefinition = "TEXT")
    private String stockMovementDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_stock_id", nullable = false)
    private MaterialStock materialStock;

    @Column(nullable = false)
    private Instant stockMovementRefresh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_created_id_user")
    private AppUser appUserCreated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_finished_id_user")
    private AppUser appUserFinished;

    @Column(nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private double inputQuantity;

    @Column(nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private double totalQuantity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String buyUnit;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestUnit;

    @Column(nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private double quantityPackage;

    @Column(nullable = false)
    private BigDecimal pricePerItem;

    @Column(nullable = false)
    private BigDecimal priceTotal;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    private Status status;

    public StockMovement(Instant stockMovementRefresh) {
        this.stockMovementRefresh = stockMovementRefresh;
    }

    public StockMovement() {
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    // método para atualizar estoque do material
    public void materialUpdate() {
        if (this.inputQuantity <= 0 || this.quantityPackage <= 0) {
            throw new IllegalArgumentException("Quantidade e embalagem devem ser maiores que zero.");
        }

        this.materialStock.addStockQuantity(this.totalQuantity);
        this.materialStock.addStockAvailable(this.totalQuantity);
        this.materialStock.setCostPerItem(this.pricePerItem);
        this.materialStock.setCostPrice(this.priceTotal);
        this.materialStock.setBuyUnit(this.buyUnit);
        this.materialStock.setRequestUnit(this.requestUnit);
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

    public MaterialStock getMaterialStock() {
        return materialStock;
    }

    public void setMaterialStock(MaterialStock material) {
        this.materialStock = material;
    }

    public Instant getStockMovementRefresh() {
        return stockMovementRefresh;
    }

    public void setStockMovementRefresh(Instant stockMovementRefresh) {
        this.stockMovementRefresh = stockMovementRefresh;
    }

    public String getBuyUnit() {
        return buyUnit;
    }

    public void setBuyUnit(String buyUnit) {
        this.buyUnit = buyUnit;
    }

    public double getQuantityPackage() {
        return quantityPackage;
    }

    public void setQuantityPackage(double quantityPackage) {
        this.quantityPackage = quantityPackage;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getPricePerItem() {
        return pricePerItem;
    }



    public void setPricePerItem(BigDecimal priceTotal, double quantity) {
        if (quantity != 0) {  // Verifica se a quantidade é diferente de 0
            // Converte quantity para BigDecimal e faz a divisão com precisão
            this.pricePerItem = priceTotal.divide(BigDecimal.valueOf(quantity), RoundingMode.HALF_UP);
        } else {
            this.pricePerItem = priceTotal;  // Se a quantidade for 0, mantém o preço total
        }
    }

    public BigDecimal getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(BigDecimal priceTotal) {
        this.priceTotal = priceTotal;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public AppUser getUserCreated() {
        return appUserCreated;
    }

    public void setUserCreated(AppUser appUserCreated) {
        this.appUserCreated = appUserCreated;
    }

    public AppUser getUserFinished() {
        return appUserFinished;
    }

    public void setUserFinished(AppUser appUserFinished) {
        this.appUserFinished = appUserFinished;
    }

    public void setInputQuantity(double inputQuantity) {
        this.inputQuantity = inputQuantity;
    }

    public Double getInputQuantity() {
        return inputQuantity;
    }

    public void setInputQuantity(Double inputQuantity) {
        this.inputQuantity = inputQuantity;
    }

    public Double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getRequestUnit() {
        return requestUnit;
    }

    public void setBuyRequest(String requestUnit) {
        this.requestUnit = requestUnit;
    }

    public void setPricePerItem(BigDecimal pricePerItem) {
        this.pricePerItem = pricePerItem;
    }
}