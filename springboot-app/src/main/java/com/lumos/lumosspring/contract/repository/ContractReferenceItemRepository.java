package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractReferenceItem;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractReferenceItemRepository extends CrudRepository<ContractReferenceItem, Long> {
    List<ContractReferenceItem> findAllByTenantId(UUID tenantId);

    List<ContractReferenceItem> findByContractReferenceItemIdIn(Collection<Long> contractReferenceItemIds);

    @Query("""
        select cri.description
        from contract_reference_item cri
        join contract_item ci on ci.contract_item_reference_id = cri.contract_reference_item_id
        where ci.contract_item_id = :contractItemId
    """)
    Optional<String> getDescription(Long contractItemId);
}
