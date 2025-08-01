package com.lumos.lumosspring.dto.stock;

public record SupplierDTO(String name,
                          String cnpj,
                          String contact,
                          String address,
                          String phone,
                          String email) {
}
