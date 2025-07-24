package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Supplier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends CrudRepository<Supplier, Long> {
    Supplier findBySupplierName(String name);
}
