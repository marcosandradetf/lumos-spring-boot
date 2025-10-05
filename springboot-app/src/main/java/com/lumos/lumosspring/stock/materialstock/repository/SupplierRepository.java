package com.lumos.lumosspring.stock.materialstock.repository;

import com.lumos.lumosspring.stock.materialstock.model.Supplier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends CrudRepository<Supplier, Long> {
    Supplier findBySupplierName(String name);
}
