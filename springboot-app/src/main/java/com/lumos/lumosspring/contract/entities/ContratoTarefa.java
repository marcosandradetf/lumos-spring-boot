package com.lumos.lumosspring.contract.entities;

import jakarta.persistence.*;

@Entity
public class ContratoTarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTarefa;

    @ManyToOne
    private Contract contract;

    @OneToOne
    private ContratoEquipe equipe;

    private int quantidadeRecebida;
    private int quantidadeExecutada;


}
