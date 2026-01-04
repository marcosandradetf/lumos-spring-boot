package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractReferenceItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ContractReferenceItemRepository extends CrudRepository<ContractReferenceItem, Long> {
    List<ContractReferenceItem> findAllByTenantId(UUID tenantId);
}
