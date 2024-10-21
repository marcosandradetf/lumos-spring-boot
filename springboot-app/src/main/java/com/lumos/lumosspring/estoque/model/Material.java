package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.*;

@Entity
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idMaterial;

    @Column(columnDefinition = "TEXT", unique = true)
    private String nomeMaterial;

    @Column(columnDefinition = "TEXT")
    private String marcaMaterial;

    @Column(columnDefinition = "TEXT")
    private String unidadeCompra;

    @Column(columnDefinition = "TEXT")
    private String unidadeRequisicao;

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private Tipo tipoMaterial;

    @ManyToOne
    @JoinColumn(name = "id_grupo")
    private Grupo grupoMaterial;

    private int qtdeEstoque;
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean inativo;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "id_almoxarifado")
    private Almoxarifado almoxarifado;

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Almoxarifado getAlmoxarifado() {
        return almoxarifado;
    }

    public void setAlmoxarifado(Almoxarifado almoxarifado) {
        this.almoxarifado = almoxarifado;
    }

    public Grupo getGrupoMaterial() {
        return grupoMaterial;
    }

    public void setGrupoMaterial(Grupo grupoMaterial) {
        this.grupoMaterial = grupoMaterial;
    }

    public Tipo getTipoMaterial() {
        return tipoMaterial;
    }

    public void setTipoMaterial(Tipo tipoMaterial) {
        this.tipoMaterial = tipoMaterial;
    }

    public Material() {}

    public long getIdMaterial() {
        return idMaterial;
    }

    public void setIdMaterial(long idMaterial) {
        this.idMaterial = idMaterial;
    }

    public String getNomeMaterial() {
        return nomeMaterial;
    }

    public void setNomeMaterial(String nomeMaterial) {
        this.nomeMaterial = nomeMaterial;
    }

    public String getUnidadeCompra() {
        return unidadeCompra;
    }

    public void setUnidadeCompra(String unidadeCompra) {
        this.unidadeCompra = unidadeCompra;
    }

    public String getUnidadeRequisicao() {
        return unidadeRequisicao;
    }

    public void setUnidadeRequisicao(String unidadeRequisicao) {
        this.unidadeRequisicao = unidadeRequisicao;
    }


    public int getQtdeEstoque() {
        return qtdeEstoque;
    }

    public void setQtdeEstoque(int qtdeEstoque) {
        this.qtdeEstoque = qtdeEstoque;
    }

    public boolean isInativo() {
        return inativo;
    }

    public void setInativo(boolean inativo) {
        this.inativo = inativo;
    }

    public String getMarcaMaterial() {
        return marcaMaterial;
    }

    public void setMarcaMaterial(String marcaMaterial) {
        this.marcaMaterial = marcaMaterial;
    }
}
