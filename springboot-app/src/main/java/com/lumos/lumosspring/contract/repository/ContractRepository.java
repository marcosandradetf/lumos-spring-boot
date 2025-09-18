package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends CrudRepository<Contract, Long> {

    Optional<Contract> findContractByContractId(long contractId);

    List<Contract> findAllByStatus(String status);

    @Query("""
        select true from contract
        where contract_id = :contractId
            and status = 'ACTIVE'
    """)
    Boolean contractIsActive(long contractId);

    @Query("""
        select coalesce(max(step), 0) as step from direct_execution
        where contract_id  = :contractId\s
   \s""")
    Integer getLastStep(long contractId);
}
