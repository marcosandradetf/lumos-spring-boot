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
    private int stockMovementId;

    @Column(columnDefinition = "TEXT")
    private String stockMovementDescription;

    @OneToOne
    private Material material;

    private Instant stockMovementRefresh;

    @OneToOne
    private User userCreated;

    @OneToOne
    private User userFinished;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int inputQuantity;

    @Column(columnDefinition = "TEXT")
    private String buyUnit;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private int quantityPackage;

    private BigDecimal pricePerItem;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(columnDefinition = "TEXT")
    private String status;

    public StockMovement(Instant stockMovementRefresh) {
        this.stockMovementRefresh = stockMovementRefresh;
    }

    public StockMovement() {
    }

    // método para atualizar estoque do material
    public void materialUpdate() {
        int stockQuantity = this.inputQuantity * this.quantityPackage;
        this.material.addStockQuantity(stockQuantity);
        this.material.addStockAvailable(stockQuantity);
        this.material.setCostPrice(this.pricePerItem);
    }


    public int getStockMovementId() {
        return stockMovementId;
    }

    public void setStockMovementId(int stockMovementId) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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