package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends CrudRepository<Contract, Long> {
   Optional<Contract> findContractByContractId(long contractId);

    List<Contract> findAllByStatus(String status);
}
