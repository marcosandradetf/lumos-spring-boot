package com.lumos.lumosspring.contrato.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class ContratoAditivoQuantitativo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idContratoAditivoQuantitativo;

    private BigDecimal valorAditivoQuantitativo;

    @OneToOne
    private Contrato contrato;

    // Método para calcular o valor total do contrato
    public void calcularValorContratoAditivoQuantitativo() {
        BigDecimal valorContrato = contrato.getValorContrato();
        this.valorAditivoQuantitativo = valorContrato.multiply(BigDecimal.valueOf(0.25));
    }

    // Método chamado antes de salvar no banco de dados
    @PrePersist
    @PreUpdate
    private void preSave() {
        calcularValorContratoAditivoQuantitativo(); // Garante o cálculo antes de salvar ou atualizar
    }





}
