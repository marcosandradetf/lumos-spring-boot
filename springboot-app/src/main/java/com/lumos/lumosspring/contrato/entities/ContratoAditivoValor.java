package com.lumos.lumosspring.contrato.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class ContratoAditivoValor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idContratoAditivoValor;

    private BigDecimal valorAditivo;

    @OneToOne
    private Contrato contrato;

    // Método para calcular o valor total do contrato
    public void calcularValorContratoAditivoValor() {
        BigDecimal valorContrato = contrato.getValorContrato();
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

    public Contrato getContrato() {
        return contrato;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

}
