package com.lumos.lumosspring.contract.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ContratoEquipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idEquipe;


}
