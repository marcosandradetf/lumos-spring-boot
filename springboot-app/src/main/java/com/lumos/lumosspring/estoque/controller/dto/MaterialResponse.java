package com.lumos.lumosspring.estoque.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lumos.lumosspring.estoque.model.Material;

@JsonInclude(JsonInclude.Include.NON_NULL)  // Isso vai garantir que valores nulos não sejam serializados
public record MaterialResponse(long idMaterial, String nomeMaterial, String marcaMaterial, String unidadeCompra, String unidadeRequisicao,
                               Integer qtdeEstoque, Boolean inativo, String tipoMaterial, String grupoMaterial, String almoxarifado, String empresa) {
    public MaterialResponse(Material material) {
        this(
                material.getIdMaterial(),
                material.getNomeMaterial(),
                material.getMarcaMaterial(),
                material.getUnidadeCompra(),
                material.getUnidadeRequisicao(),
                material.getQtdeEstoque(),
                material.isInativo(),
                material.getTipoMaterial() != null ? material.getTipoMaterial().getNomeTipo() : null,
                material.getTipoMaterial() != null ? material.getTipoMaterial().getGrupo().getNomeGrupo(): null,
                material.getAlmoxarifado() != null ? material.getAlmoxarifado().getNomeAlmoxarifado() : null,
                material.getEmpresa() != null ? material.getEmpresa().getNomeEmpresa() : null
        );
    }
}
