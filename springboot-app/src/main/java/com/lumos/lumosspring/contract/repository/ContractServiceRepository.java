package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractItem;
import com.lumos.lumosspring.contract.entities.ContractService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractServiceRepository extends JpaRepository<ContractService, Long> {
}
