package com.lumos.lumosspring.company.repository;

import com.lumos.lumosspring.company.model.Company;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends CrudRepository<Company, Long> {
    Iterable<Company> findAllByTenantId(UUID tenantId);
}
