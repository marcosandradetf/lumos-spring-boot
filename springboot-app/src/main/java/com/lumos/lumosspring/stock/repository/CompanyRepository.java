package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Company;
import org.springframework.data.repository.CrudRepository;

public interface CompanyRepository extends CrudRepository<Company, Long> {
}
