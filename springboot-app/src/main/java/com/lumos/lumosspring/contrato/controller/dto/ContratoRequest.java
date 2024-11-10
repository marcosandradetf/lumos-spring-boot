package com.lumos.lumosspring.contrato.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record ContratoRequest(String numeroContrato, String descricaoContrato, String uf, String city, List<Long> idMaterial, List<Integer> qtde, List<String> valor) {
}
