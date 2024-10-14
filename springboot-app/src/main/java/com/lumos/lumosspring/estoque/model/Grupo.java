package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idGrupo;

    private String nomeAlmoxarifadoo;

    public long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNomeAlmoxarifadoo() {
        return nomeAlmoxarifadoo;
    }

    public void setNomeAlmoxarifadoo(String nomeAlmoxarifadoo) {
        this.nomeAlmoxarifadoo = nomeAlmoxarifadoo;
    }
}
