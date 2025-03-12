package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.contract.entities.ContractItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractItemRepository extends JpaRepository<ContractItem, Long> {
}
