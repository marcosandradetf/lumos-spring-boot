package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Almoxarifado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idAlmoxarifado;
    private String nomeAlmoxarifado;

    public long getIdAlmoxarifado() {
        return idAlmoxarifado;
    }

    public void setIdAlmoxarifado(long idAlmoxarifado) {
        this.idAlmoxarifado = idAlmoxarifado;
    }

    public String getNomeAlmoxarifado() {
        return nomeAlmoxarifado;
    }

    public void setNomeAlmoxarifado(String nomeAlmoxarifado) {
        this.nomeAlmoxarifado = nomeAlmoxarifado;
    }
}
