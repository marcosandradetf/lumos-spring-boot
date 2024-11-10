package com.lumos.lumosspring.contrato.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record ContratoResponse(List<Long> idMaterial, List<Integer> qtde, List<BigDecimal> valor) {
}
