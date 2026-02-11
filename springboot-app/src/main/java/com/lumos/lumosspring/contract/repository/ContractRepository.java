package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.dto.ContractItemBalance;
import com.lumos.lumosspring.contract.entities.Contract;
import kotlin.Triple;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends CrudRepository<Contract, Long> {

    Optional<Contract> findContractByContractId(long contractId);

    @Query("""
        select
            c.contract_id,
            c.contract_number as number,
            c.contractor,
            c.address,
            c.phone,
            c.cnpj,
            c.contract_file,
            creator.name || ' ' || creator.last_name as created_by,
            count(ci.*) as item_quantity,
            c.status as contract_status,
            SUM(ci.total_price) AS contract_value,
            c.company_id,
            updated.name || ' ' || updated.last_name as last_updated_by
        from contract c
        join contract_item ci on ci.contract_contract_id = c.contract_id
        join app_user creator on c.created_by_id_user = creator.user_id
        left join app_user updated on c.last_updated_by = updated.user_id
        where
            (
                (:contractor IS NOT NULL AND c.tenant_id = :tenantId AND (lower(c.contractor) like '%' || :contractor || '%' OR c.contract_number = :contractor ))
                OR (:contractor IS NULL AND c.tenant_id = :tenantId
                    AND c.status = :status
                    AND c.creation_date >= :start
                    AND c.creation_date < :end)
            )
        group by c.contract_id, c.contractor, creator.name, creator.last_name, updated.name, updated.last_name
        order by c.contractor
    """)
    List<ContractResponseDTO> findAllByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") String status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("contractor") String contractor
    );
    record ContractResponseDTO (
            long contractId,
            String number,
            String contractor,
            String address,
            String phone,
            String cnpj,
            String contractFile,
            String createdBy,
            int itemQuantity,
            String contractStatus,
            BigDecimal contractValue,
            long companyId,
            String lastUpdatedBy
    ){}

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
        select distinct c.contract_id, upper(c.contractor) as contractor, 'INSTALLATION' as type, c.contract_number
        from direct_execution de
        join contract c on de.contract_id = c.contract_id
        where de.direct_execution_status = 'FINISHED' and de.tenant_id = :tenantId
        UNION
        select distinct c.contract_id, upper(c.contractor) as contractor, 'INSTALLATION' as type, c.contract_number
        from pre_measurement p
        join contract c on p.contract_contract_id = c.contract_id
        where p.status = 'FINISHED' and p.tenant_id = :tenantId
        UNION
        select distinct c.contract_id, upper(c.contractor) as contractor, 'MAINTENANCE' as type, c.contract_number
        from maintenance m
        join contract c on c.contract_id = m.contract_id
        where m.status = 'FINISHED' and m.tenant_id = :tenantId
        order by contractor
    """
    )
    List<ContractWithExecutionResponse> getContractsWithExecution(UUID tenantId);
    record ContractWithExecutionResponse(Long contractId, String contractor, String type, String contractNumber){}
}