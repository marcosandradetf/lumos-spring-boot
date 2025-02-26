package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material_type", nullable = false)
    private Type materialType;


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "id_company", nullable = false)
//    private Company company;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "id_deposit", nullable = false)
//    private Deposit deposit;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MaterialStock> materialStocks;
//
//    public Company getCompany() {
//        return company;
//    }
//
//    public void setCompany(Company company) {
//        this.company = company;
//    }
//
//    public Deposit getDeposit() {
//        return deposit;
//    }
//
//    public void setDeposit(Deposit almoxarifado) {
//        this.deposit = almoxarifado;
//    }

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


    public String getMaterialBrand() {
        return materialBrand;
    }

    public void setMaterialBrand(String marcaMaterial) {
        this.materialBrand = marcaMaterial;
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

    public Set<MaterialStock> getMaterialStocks() {
        return materialStocks;
    }

    public void setMaterialStocks(Set<MaterialStock> productStocks) {
        this.materialStocks = productStocks;
    }
}

