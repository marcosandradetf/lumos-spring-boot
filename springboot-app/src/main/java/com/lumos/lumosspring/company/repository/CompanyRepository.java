package com.lumos.lumosspring.company.repository;

import com.lumos.lumosspring.company.model.Company;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends CrudRepository<Company, Long> {
    Iterable<Company> findAllByTenantId(UUID tenantId);

    @Query("""
            SELECT
                *
            FROM company
            WHERE tenant_id = :tenantId
            ORDER BY 1
            LIMIT 1
    """)
    Optional<Company> getMainCompanyByTenantId(UUID tenantId);
}
