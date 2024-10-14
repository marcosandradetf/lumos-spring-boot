package com.lumos.lumosspring.estoque.model;

import jakarta.persistence.*;

@Entity
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idMaterial;

    private String nomeMaterial;
    private String unidadeCompra;
    private String unidadeRequisicao;

    @ManyToOne
    @JoinColumn(name = "tipo_material_id_tipo")
    private Tipo tipoMaterial;

    @ManyToOne
    @JoinColumn(name = "grupo_material_id_grupo")
    private Grupo grupoMaterial;

    private int qtdeEstoque;
    private boolean inativo;

    @ManyToOne
    @JoinColumn(name = "empresa_id_empresa")
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "almoxarifado_id_almoxarifado")
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

}
