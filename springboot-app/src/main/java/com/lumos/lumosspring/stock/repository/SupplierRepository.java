package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Supplier findBySupplierName(String name);
}
