package com.lumos.lumosspring.stock.controller.dto;

public record SupplierDTO(String name,
                          String cnpj,
                          String contact,
                          String address,
                          String phone,
                          String email) {
}
