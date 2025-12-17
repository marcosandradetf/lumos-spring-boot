package com.lumos.lumosspring.directexecution.repository

import com.lumos.lumosspring.directexecution.model.*
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
interface DirectExecutionRepository : CrudRepository<DirectExecution, Long> {
    data class InstallationRow(val status: String, val description: String)

    @Query("""
        SELECT direct_execution_status as status, description
        FROM direct_execution 
        WHERE direct_execution_id = :id
    """)
    fun getInstallation(id: Long): InstallationRow?

    @Modifying
    @Query(
        """
        UPDATE direct_execution 
        SET direct_execution_status = :status,
            responsible = :responsible,
            signature_uri = :signatureUri,
            sign_date = :signDate
        WHERE direct_execution_id = :id
    """
    )
    fun finishDirectExecution(
        @Param("id") id: Long,
        @Param("status") status: String,
        @Param("signatureUri") signatureUri: String?,
        @Param("signDate") signDate: Instant?,
        @Param("responsible") responsible: String?
    )
}

@Repository
interface DirectExecutionRepositoryItem : CrudRepository<DirectExecutionItem, Long> {
    data class InstallationRow(val contractItemId: Long, val measuredItemQuantity: BigDecimal, val factor: BigDecimal?)

    @Query(
        """
        select dei.contract_item_id, dei.measured_item_quantity, cri.factor 
        from direct_execution_item dei
        join contract_item ci on ci.contract_item_id = dei.contract_item_id
        join public.contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
        where dei.direct_execution_id = :direct_execution_id
    """
    )
    fun getByDirectExecutionId(directExecutionId: Long): List<InstallationRow>

    @Query(
    """
            select
                round(dei.measured_item_quantity / cast(:size as numeric), 2) as quantity,
                dei.measured_item_quantity - round(dei.measured_item_quantity / cast(:size as numeric), 2) * :size as correction,
                ci.contract_item_id
            from direct_execution_item dei
            join contract_item ci on ci.contract_item_id = dei.contract_item_id
            join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
            where cri.type = 'CABO'
                and dei.direct_execution_id = :directExecutionId
        """
    )
    fun getCableDistribution(size: Int, directExecutionId: Long): Triple<BigDecimal, BigDecimal, Long>
}

@Repository
interface DirectExecutionRepositoryStreet : CrudRepository<DirectExecutionStreet, Long> {
    @Query("""
        select direct_execution_street_id
        from direct_execution_street
        where direct_execution_id = :id
    """)
    fun getByDirectExecutionId(id: Long): List<Long>
}

@Repository
interface DirectExecutionRepositoryStreetItem : CrudRepository<DirectExecutionStreetItem, Long> {
}

@Repository
interface DirectExecutionExecutorRepository : CrudRepository<DirectExecutionExecutor, Long> {
}