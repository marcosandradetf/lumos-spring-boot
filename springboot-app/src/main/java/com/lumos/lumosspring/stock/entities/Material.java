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

    @Column(columnDefinition = "TEXT")
    private String materialName;

    @Column(columnDefinition = "TEXT")
    private String materialBrand;

    private Float materialPower;

    @Column(columnDefinition = "TEXT")
    private String buyUnit;

    @Column(columnDefinition = "TEXT")
    private String requestUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material_type")
    private Type materialType;

    @OneToMany
    @JoinColumn(name = "id_contract")
    private List<Contract> contracts;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int stockQuantity;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int stockAvailable;

    private BigDecimal costPrice;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean inactive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_company")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposit")
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

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContrato(Contract contract) {
        this.contracts.add(contract);
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

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int qtdeEstoque) {
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

    public void removeStockAvailable(int quantityAvailable) {
        this.stockAvailable -= quantityAvailable;
    }

    public Float getMaterialPower() {
        return materialPower;
    }

    public void setMaterialPower(Float materialPower) {
        this.materialPower = materialPower;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }

    public int getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(int stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
}
