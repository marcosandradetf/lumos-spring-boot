package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

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

    private double material_quantity;

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

    public double getMaterial_quantity() {
        return material_quantity;
    }

    public void setMaterial_quantity(double material_quantity) {
        this.material_quantity = material_quantity;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
