package com.lumos.lumosspring.contrato.entities;

import jakarta.persistence.*;

@Entity
public class ContratoTarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTarefa;

    @ManyToOne
    private Contrato contrato;

    @OneToOne
    private ContratoEquipe equipe;

    private int quantidadeRecebida;
    private int quantidadeExecutada;


}
