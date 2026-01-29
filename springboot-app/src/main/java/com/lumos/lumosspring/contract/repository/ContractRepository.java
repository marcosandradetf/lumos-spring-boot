package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.dto.ContractItemBalance;
import com.lumos.lumosspring.contract.entities.Contract;
import kotlin.Triple;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    @Query(
        """
            select ci.contract_item_id, ci.contracted_quantity - ci.quantity_executed as current_balance, coalesce(cri.name_for_import, cri.description) as item_name
            from contract_item ci
            join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
            where ci.contract_contract_id = :contractId
        """
    )
    List<ContractItemBalance> getContractItemBalance(long contractId);

    @Query(
            """
        select distinct c.contract_id, c.contractor, 'INSTALLATION' as type
        from direct_execution de
        join contract c on de.contract_id = c.contract_id
        where de.direct_execution_status = 'FINISHED' and de.tenant_id = :tenantId
        UNION
        select distinct c.contract_id, c.contractor, 'INSTALLATION' as type
        from pre_measurement p
        join contract c on p.contract_contract_id = c.contract_id
        where p.status = 'FINISHED' and p.tenant_id = :tenantId
        UNION
        select distinct c.contract_id, c.contractor, 'INSTALLATION' as type
        from maintenance m
        join contract c on c.contract_id = m.contract_id
        where m.status = 'FINISHED' and m.tenant_id = :tenantId
        order by contractor
    """
    )
    List<ContractWithExecutionResponse> getContractsWithExecution(UUID tenantId);
    record ContractWithExecutionResponse(Long contractId, String contractor, String type){}
}