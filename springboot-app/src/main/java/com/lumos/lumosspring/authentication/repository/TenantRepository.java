package com.lumos.lumosspring.authentication.repository;

import com.lumos.lumosspring.authentication.model.Tenant;
import org.springframework.data.repository.CrudRepository;
import java.util.UUID;

public interface TenantRepository extends CrudRepository<Tenant, UUID> {
}
