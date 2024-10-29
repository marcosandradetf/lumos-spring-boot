package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_grupos")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idGrupo;

    @Column(columnDefinition = "TEXT", unique = true)
    private String nomeGrupo;

    public long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNomeGrupo() {
        return nomeGrupo;
    }

    public void setNomeGrupo(String nomeGrupo) {
        this.nomeGrupo = nomeGrupo;
    }
}
