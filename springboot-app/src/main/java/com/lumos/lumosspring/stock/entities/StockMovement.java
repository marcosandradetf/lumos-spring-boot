package com.lumos.lumosspring.stock.entities;

import com.lumos.lumosspring.authentication.entities.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tb_stock_movement")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_movement_id")
    private Long stockMovementId;

    @Column(columnDefinition = "TEXT")
    private String stockMovementDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Instant stockMovementRefresh;

    @ManyToOne(fetch = FetchType.LAZY)
    private User userCreated;

    @ManyToOne(fetch = FetchType.LAZY)
    private User userFinished;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int inputQuantity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String buyUnit;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private int quantityPackage;

    @Column(nullable = false)
    private BigDecimal pricePerItem;

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
        int stockQuantity = this.inputQuantity * this.quantityPackage;
        this.material.addStockQuantity(stockQuantity);
        this.material.addStockAvailable(stockQuantity);
        this.material.setCostPrice(this.pricePerItem);
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

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Instant getStockMovementRefresh() {
        return stockMovementRefresh;
    }

    public void setStockMovementRefresh(Instant stockMovementRefresh) {
        this.stockMovementRefresh = stockMovementRefresh;
    }

    public int getInputQuantity() {
        return inputQuantity;
    }

    public void setInputQuantity(int inputQuantity) {
        this.inputQuantity = inputQuantity;
    }

    public String getBuyUnit() {
        return buyUnit;
    }

    public void setBuyUnit(String buyUnit) {
        this.buyUnit = buyUnit;
    }

    public int getQuantityPackage() {
        return quantityPackage;
    }

    public void setQuantityPackage(int quantityPackage) {
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

    public void setPricePerItem(BigDecimal pricePerItem) {
        this.pricePerItem = pricePerItem;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(User userCreated) {
        this.userCreated = userCreated;
    }

    public User getUserFinished() {
        return userFinished;
    }

    public void setUserFinished(User userFinished) {
        this.userFinished = userFinished;
    }

}