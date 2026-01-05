package com.lumos.lumosspring.contract.repository

import com.lumos.lumosspring.contract.entities.ContractItemService
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal

interface ContractItemServiceRepository: CrudRepository<ContractItemService, Long> {
    data class ContractItemServiceRow(val contractItemId: Long, val factor: BigDecimal)

    @Query("""
        select 
            dei.contract_item_id, cis.factor
        from contract_reference_item cri
        join contract_item ci on ci.contract_item_reference_id = cri.contract_reference_item_id
        join contract_item_service cis on cis.contract_item_reference_id = cri.contract_reference_item_id
        join contract_item ci2 on ci2.contract_item_reference_id = cis.contract_item_reference_id_service
        join direct_execution_item dei on dei.contract_item_id = ci2.contract_item_id
        where ci.contract_item_id = :contractItemId and direct_execution_id = :directExecutionId;
    """)
    fun getAllById(contractItemId: Long, directExecutionId: Long): List<ContractItemServiceRow>
}