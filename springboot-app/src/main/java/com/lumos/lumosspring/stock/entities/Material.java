package com.lumos.lumosspring.stock.entities;

import com.lumos.lumosspring.contract.entities.Contract;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "tb_materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private long idMaterial;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String materialName;

    @Column(columnDefinition = "TEXT")
    private String materialBrand;

    private String materialPower;

    private String materialAmps;

    private String materialLength;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String buyUnit;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material_type", nullable = false)
    private Type materialType;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private float stockQuantity;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private float stockAvailable;

    private BigDecimal costPrice;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean inactive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_company", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposit", nullable = false)
    private Deposit deposit;

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Deposit getDeposit() {
        return deposit;
    }

    public void setDeposit(Deposit almoxarifado) {
        this.deposit = almoxarifado;
    }

    public Type getMaterialType() {
        return materialType;
    }

    public void setMaterialType(Type typeMaterial) {
        this.materialType = typeMaterial;
    }

    public Material() { }

    public long getIdMaterial() {
        return idMaterial;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String nomeMaterial) {
        this.materialName = nomeMaterial;
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

    public float getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(float qtdeEstoque) {
        this.stockQuantity = qtdeEstoque;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inativo) {
        this.inactive = inativo;
    }

    public String getMaterialBrand() {
        return materialBrand;
    }

    public void setMaterialBrand(String marcaMaterial) {
        this.materialBrand = marcaMaterial;
    }

    public void addStockQuantity(int quantityCompleted) {
        this.stockQuantity += quantityCompleted;
    }

    public void addStockAvailable(int quantityAvailable) {
        this.stockAvailable += quantityAvailable;
    }

    public void removeStockQuantity(int quantityCompleted) {
        this.stockQuantity -= quantityCompleted;
    }

    public void removeStockAvailable(float quantityAvailable) {
        this.stockAvailable -= quantityAvailable;
    }

    public String getMaterialPower() {
        return materialPower;
    }

    public void setMaterialPower(String materialPower) {
        this.materialPower = materialPower;
    }

    public float getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(float stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
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
}

