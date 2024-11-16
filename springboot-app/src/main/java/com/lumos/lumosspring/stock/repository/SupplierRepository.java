package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
