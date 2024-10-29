package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_almoxarifados")
public class Almoxarifado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idAlmoxarifado;
    @Column(columnDefinition = "TEXT", unique = true)
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
