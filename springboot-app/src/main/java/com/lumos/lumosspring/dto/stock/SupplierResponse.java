package com.lumos.lumosspring.dto.stock;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lumos.lumosspring.stock.entities.Supplier;

@JsonInclude(JsonInclude.Include.NON_NULL)  // Isso vai garantir que valores nulos não sejam serializados
public record SupplierResponse(Long id, String name) {
    public SupplierResponse(Supplier supplier) {
        this(
                supplier.getSupplierId(),
                supplier.getSupplierName()
        );
    }
}
