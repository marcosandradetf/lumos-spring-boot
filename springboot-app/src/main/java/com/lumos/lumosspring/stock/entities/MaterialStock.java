package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_material_stock")
public class MaterialStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialIdStock;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne
    @JoinColumn(name = "deposit_id", nullable = false)
    private Deposit deposit;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String buyUnit;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestUnit;

    @Column(nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private double stockQuantity;

    @Column(nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private double stockAvailable;

    private BigDecimal costPerItem;

    private BigDecimal costPrice;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean inactive;


    public Long getMaterialIdStock() {
        return materialIdStock;
    }

    public void setMaterialIdStock(Long productIdStock) {
        this.materialIdStock = productIdStock;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Deposit getDeposit() {
        return deposit;
    }

    public void setDeposit(Deposit deposit) {
        this.deposit = deposit;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }





    public String getBuyUnit() {
        return buyUnit;
    }

    public void setBuyUnit(String unidadeCompra) {
        this.buyUnit = unidadeCompra;
    }

    public String getRequestUnit() {
        return requestUnit;
    }

    public void setRequestUnit(String unidadeRequisicao) {
        this.requestUnit = unidadeRequisicao;
    }

    public double getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(double qtdeEstoque) {
        this.stockQuantity = qtdeEstoque;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inativo) {
        this.inactive = inativo;
    }

    public void addStockQuantity(double quantityCompleted) {
        this.stockQuantity += quantityCompleted;
    }

    public void addStockAvailable(double quantityAvailable) {
        this.stockAvailable += quantityAvailable;
    }

    public void removeStockQuantity(int quantityCompleted) {
        this.stockQuantity -= quantityCompleted;
    }

    public void removeStockAvailable(double quantityAvailable) {
        this.stockAvailable -= quantityAvailable;
    }

    public double getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(double stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getCostPerItem() {
        return costPerItem;
    }

    public void setCostPerItem(BigDecimal costPerItem) {
        this.costPerItem = costPerItem;
    }
}
