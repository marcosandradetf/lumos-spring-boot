package com.lumos.lumosspring.contract.repository;

import com.lumos.lumosspring.contract.entities.ContractItem;
import com.lumos.lumosspring.stock.order.installationrequest.dto.ResponseItemReserve;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ContractItemsQuantitativeRepository extends CrudRepository<ContractItem, Long> {
    record ItemDistributionRow(BigDecimal quantity, BigDecimal correction,  Long contractItemId){}

    void deleteByContractId(Long contractId);

    @Query("""
        select\s
        	ci.contracted_quantity - ci.quantity_executed as balance
        from contract_item ci
        where ci.contract_item_id = :contractItemId
    """)
    BigDecimal getBalance(long contractItemId);

    @Query("""
        select\s
        	ci.contracted_quantity - ci.quantity_executed -\s
        	(
        		select coalesce(sum(mr.reserved_quantity - mr.quantity_completed),0)
        		from material_reservation mr\s
        		where mr.contract_item_id = ci.contract_item_id\s
        			and mr.status <> 'FINISHED' and mr.quantity_completed < mr.reserved_quantity\s
        	) as balance
        from contract_item ci
        where ci.contract_item_id = :contractItemId
    """)
    BigDecimal getTotalBalance(long contractItemId);

    @Query("""
        select mr.material_id_reservation, coalesce(de.description, p.city) as description, mr.reserved_quantity,\s
        	mr.quantity_completed, de.direct_execution_id, p.pre_measurement_id,\s
        	cri.description as item_name
        from material_reservation mr\s
        join contract_item ci on ci.contract_item_id = mr.contract_item_id\s
        join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
        left join direct_execution de on de.direct_execution_id  = mr.direct_execution_id\s
        left join pre_measurement p on p.pre_measurement_id = mr.pre_measurement_id\s
        where mr.status <> 'FINISHED' and mr.quantity_completed < mr.reserved_quantity and mr.contract_item_id  = :contractItemId
   \s""")
    List<ResponseItemReserve> getInProgressReservations(long contractItemId);

    @Modifying
    @Query("""
        update contract_item
        set quantity_executed = quantity_executed + :quantityExecuted * :factor
        where contract_item_id = :contractItemId
    """)
    void updateBalance(long contractItemId, BigDecimal quantityExecuted, BigDecimal factor);

    @Query(
            """
                select
                    round(dei.measured_item_quantity / cast(:size as numeric), 2) as quantity,
                    dei.measured_item_quantity - round(dei.measured_item_quantity / cast(:size as numeric), 2) * :size as correction,
                    ci.contract_item_id
                from direct_execution_item dei
                join contract_item ci on ci.contract_item_id = dei.contract_item_id
                join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
                where cri.description = :description
                    and dei.direct_execution_id = :directExecutionId
            """
    )
    ItemDistributionRow getItemDistribution(Integer size, Long directExecutionId, String description);

}
