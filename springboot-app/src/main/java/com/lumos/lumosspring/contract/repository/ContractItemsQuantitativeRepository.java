package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractItemsQuantitativeRepository extends JpaRepository<ContractItem, Long> {
     List<ContractItem> findByContract_ContractId(long contractContractId);
}
