package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Long idMaterial;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String materialName;

    @Column(columnDefinition = "TEXT")
    private String materialBrand;

    private String materialPower;

    private String materialAmps;

    private String materialLength;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material_type", nullable = false)
    private Type materialType;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MaterialStock> materialStocks;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_related_materials",
            joinColumns = @JoinColumn(name = "material_id"),
            inverseJoinColumns = @JoinColumn(name = "related_id")
    )
    private Set<Material> relatedMaterials = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "tb_materials_services_materials",
            joinColumns = @JoinColumn(name = "material_id"),
            inverseJoinColumns = @JoinColumn(name = "material_service_id")
    )
    private Set<MaterialService> materialServices = new HashSet<>();

    public Type getMaterialType() {
        return materialType;
    }

    public void setMaterialType(Type typeMaterial) {
        this.materialType = typeMaterial;
    }

    public Material() { }

    public Long getIdMaterial() {
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

    public Set<Material> getRelatedMaterials() {
        return relatedMaterials;
    }

    public void setRelatedMaterials(Set<Material> relatedMaterials) {
        this.relatedMaterials = relatedMaterials;
    }

    public Set<MaterialService> getMaterialServices() {
        return materialServices;
    }

    public void setMaterialServices(Set<MaterialService> materialServices) {
        this.materialServices = materialServices;
    }
}

