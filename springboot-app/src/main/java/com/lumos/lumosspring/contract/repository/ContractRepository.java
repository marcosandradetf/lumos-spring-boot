package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @EntityGraph(attributePaths = {"contractItemsQuantitative"})
    Optional<Contract> findContractByContractId(long contractId);
}
