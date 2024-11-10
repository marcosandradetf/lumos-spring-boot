package com.lumos.lumosspring.estoque.controller.dto;

import com.lumos.lumosspring.estoque.model.Material;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record MaterialRequest(String nomeMaterial, String marcaMaterial, String unidadeCompra, String unidadeRequisicao,
                              Integer qtdeEstoque, Boolean inativo, Long tipoMaterial, Long almoxarifado, Long empresa) {

}
