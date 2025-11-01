package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.Contract;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends CrudRepository<Contract, Long> {

    Optional<Contract> findContractByContractId(long contractId);

    List<Contract> findAllByTenantIdAndStatus(UUID tenantId, String status);

    @Query("""
        select true from contract
        where contract_id = :contractId
            and status = 'ACTIVE'
    """)
    Boolean contractIsActive(long contractId);

    @Query("""
                 with cte as (
                 	select coalesce(max(step), 0) as step\s
                 	from direct_execution
                 	where contract_id  = :contractId
                 	union\s
                 	select coalesce(max(step), 0) as step\s
                 	from pre_measurement\s
                 	where contract_contract_id  = :contractId
                 )
                 select coalesce(max(step), 0) as step\s
                 from cte
            \s""")
    Integer getLastStep(long contractId);
}
