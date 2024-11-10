package com.lumos.lumosspring.contrato.entities;

import com.lumos.lumosspring.estoque.model.Material;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class ContratoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idContratoItem;
    private int quatidadeItem;
    private BigDecimal valorItem = BigDecimal.ZERO;
    private BigDecimal valorTotalItem = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "id_material")
    private Material material;

    @ManyToOne
    @JoinColumn(name = "id_contrato")
    private Contrato contrato;

    // Método para calcular o valor total
    private void calcularValorTotal() {
        this.valorTotalItem = this.valorItem.multiply(BigDecimal.valueOf(this.quatidadeItem));
        // Verifica se o valor do contrato não é nulo e, se for, define-o como zero antes de somar
        if (this.contrato.getValorContrato() == null) {
            this.contrato.setValorContrato(BigDecimal.ZERO);
        }
        // Adiciona o valor total do item ao valor total do contrato
        this.contrato.setValorContrato(this.contrato.getValorContrato().add(this.valorTotalItem));
    }

    // Método chamado antes de salvar no banco de dados
    @PrePersist
    @PreUpdate
    private void preSave() {
        calcularValorTotal(); // Garante o cálculo antes de salvar ou atualizar
    }

    public int getIdContratoItem() {
        return idContratoItem;
    }

    public void setIdContratoItem(int idContratoItem) {
        this.idContratoItem = idContratoItem;
    }

    public int getQuatidadeItem() {
        return quatidadeItem;
    }

    public void setQuatidadeItem(int quatidadeItem) {
        this.quatidadeItem = quatidadeItem;
    }

    public BigDecimal getValorItem() {
        return valorItem;
    }

    public void setValorItem(BigDecimal valorItem) {
        this.valorItem = valorItem;
    }

    public BigDecimal getValorTotalItem() {
        return valorTotalItem;
    }

    public void setValorTotalItem(BigDecimal valorTotalItem) {
        this.valorTotalItem = valorTotalItem;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
