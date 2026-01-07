package com.lumos.lumosspring.contract.repository

import com.lumos.lumosspring.contract.entities.ContractItemDependency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal

interface ContractItemDependencyRepository: CrudRepository<ContractItemDependency, Long> {
    data class ContractItemDependencyRow(val contractItemId: Long, val factor: BigDecimal)

    @Query("""
        select 
            dei.contract_item_id, cid.factor
        from contract_reference_item cri
        join contract_item ci on ci.contract_item_reference_id = cri.contract_reference_item_id
        join contract_item_dependency cid on cid.contract_item_reference_id = cri.contract_reference_item_id
        join contract_item ci2 on ci2.contract_item_reference_id = cid.contract_item_reference_id_dependency
        join direct_execution_item dei on dei.contract_item_id = ci2.contract_item_id
        where ci.contract_item_id = :contractItemId and dei.direct_execution_id = :directExecutionId;
    """)
    fun getAllDirectExecutionItemsById(contractItemId: Long, directExecutionId: Long): List<ContractItemDependencyRow>

    @Query("""
        select 
            psi.contract_item_id, cid.factor
        from contract_reference_item cri
        join contract_item ci on ci.contract_item_reference_id = cri.contract_reference_item_id
        join contract_item_dependency cid on cid.contract_item_reference_id = cri.contract_reference_item_id
        join contract_item ci2 on ci2.contract_item_reference_id = cid.contract_item_reference_id_dependency
        join pre_measurement_street_item psi on psi.contract_item_id = ci2.contract_item_id
        where ci.contract_item_id = :contractItemId and psi.pre_measurement_street_id = :preMeasurementStreetId;
    """)
    fun getAllPreMeasurementItemsById(contractItemId: Long, preMeasurementStreetId: Long): List<ContractItemDependencyRow>
}