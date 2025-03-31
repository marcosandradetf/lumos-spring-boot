package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractItemsQuantitativeRepository extends JpaRepository<ContractItemsQuantitative, Long> {
     List<ContractItemsQuantitative> findByContract_ContractId(long contractContractId);
}
