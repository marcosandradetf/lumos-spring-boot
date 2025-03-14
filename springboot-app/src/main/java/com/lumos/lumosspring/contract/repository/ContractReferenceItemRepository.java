package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractReferenceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractReferenceItemRepository extends JpaRepository<ContractReferenceItem, Long> {
}
