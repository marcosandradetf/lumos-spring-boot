package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_tipos")
public class Tipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idTipo;
    @Column(columnDefinition = "TEXT", unique = true)
    private String nomeTipo;
    @ManyToOne
    @JoinColumn(name = "id_grupo")
    private Grupo grupo;

    public long getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(long idTipo) {
        this.idTipo = idTipo;
    }

    public String getNomeTipo() {
        return nomeTipo;
    }

    public void setNomeTipo(String nomeTipo) {
        this.nomeTipo = nomeTipo;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}
