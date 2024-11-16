package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.stock.entities.Material;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private int idItem;
    private int itemQuantity;
    private BigDecimal itemValue = BigDecimal.ZERO;
    private BigDecimal itemTotalValue = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "id_material")
    private Material material;

    @ManyToOne
    @JoinColumn(name = "id_contract")
    private Contract contract;

    // Método para calcular o valor total do contrato
    private void calculateContractTotalValue() {
        this.itemTotalValue = this.itemValue.multiply(BigDecimal.valueOf(this.itemQuantity));
        // Verifica se o valor do contrato não é nulo e, se for, define-o como zero antes de somar
        if (this.contract.getContractValue() == null) {
            this.contract.setContractValue(BigDecimal.ZERO);
        }
        // Adiciona o valor total do item ao valor total do contrato
        this.contract.setContractValue(this.contract.getContractValue().add(this.itemTotalValue));
    }

    private void removeStockAvailable() {
        int qtStockAvailable = this.material.getStockAvailable();
        if (qtStockAvailable > 0) {
            this.material.removeStockAvailable(this.itemQuantity);
        }
    }

    // Método chamado antes de salvar no banco de dados
    @PrePersist
    @PreUpdate
    private void preSave() {
        calculateContractTotalValue(); // Garante o cálculo antes de salvar ou atualizar
        removeStockAvailable();
    }

    public int getItemId() {
        return idItem;
    }

    public void setItemId(int itemId) {
        this.idItem = itemId;
    }

    public int getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(int itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public BigDecimal getItemValue() {
        return itemValue;
    }

    public void setItemValue(BigDecimal itemValue) {
        this.itemValue = itemValue;
    }

    public BigDecimal getItemTotalValue() {
        return itemTotalValue;
    }

    public void setItemTotalValue(BigDecimal itemTotalValue) {
        this.itemTotalValue = itemTotalValue;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Contract getContract() {
        return contract;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
