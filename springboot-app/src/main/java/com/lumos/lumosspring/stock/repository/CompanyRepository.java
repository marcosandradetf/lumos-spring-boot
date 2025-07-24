package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Company;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends CrudRepository<Company, Long> {
}
