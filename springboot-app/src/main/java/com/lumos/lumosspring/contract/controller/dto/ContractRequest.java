package com.lumos.lumosspring.contract.controller.dto;

import java.util.List;

public record ContractRequest(String numeroContrato, String descricaoContrato, String uf, String city, List<Long> idMaterial, List<Integer> qtde, List<String> valor) {
}
