package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.stock.entities.MaterialStock;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private long itemId;

    @ManyToOne
    @JoinColumn(name = "material_stock_id")
    private MaterialStock materialStock;

    @ManyToOne
    @JoinColumn(name = "pre_measurement_id")
    private PreMeasurement preMeasurement;

    private double itemQuantity;
    private BigDecimal itemValue = BigDecimal.ZERO;
    private BigDecimal itemTotalValue = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "contract_id")
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
        double qtStockAvailable = this.materialStock.getStockAvailable();
        if (qtStockAvailable > 0) {
            this.materialStock.removeStockAvailable(this.itemQuantity);
        }
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public double getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(double itemQuantity) {
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

    public MaterialStock getMaterialStock() {
        return materialStock;
    }

    public void setMaterialStock(MaterialStock material) {
        this.materialStock = material;
    }

    public PreMeasurement getMeasurement() {
        return preMeasurement;
    }

    public void setMeasurement(PreMeasurement measurement) {
        this.preMeasurement = measurement;
    }
}
