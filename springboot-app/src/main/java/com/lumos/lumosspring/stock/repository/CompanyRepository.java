package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
