package com.lumos.lumosspring.contract.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class ContratoAditivoValor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idContratoAditivoValor;

    private BigDecimal valorAditivo;

    @OneToOne
    private Contract contract;

    // Método para calcular o valor total do contrato
    public void calcularValorContratoAditivoValor() {
        BigDecimal valorContrato = contract.getContractValue();
        this.valorAditivo = valorContrato.multiply(BigDecimal.valueOf(0.25));
    }

    // Método chamado antes de salvar no banco de dados
    @PrePersist
    @PreUpdate
    private void preSave() {
        calcularValorContratoAditivoValor(); // Garante o cálculo antes de salvar ou atualizar
    }

    public Long getIdContratoAditivoValor() {
        return idContratoAditivoValor;
    }

    public void setIdContratoAditivoValor(Long idContratoAditivoValor) {
        this.idContratoAditivoValor = idContratoAditivoValor;
    }

    public BigDecimal getValorAditivo() {
        return valorAditivo;
    }

    public void setValorAditivo(BigDecimal valorAditivo) {
        this.valorAditivo = valorAditivo;
    }

    public Contract getContrato() {
        return contract;
    }

    public void setContrato(Contract contract) {
        this.contract = contract;
    }

}
