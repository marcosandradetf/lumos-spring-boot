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

    @Query("select direct_execution_status as status, description from direct_execution where direct_execution_id = :id")
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
}

@Repository
interface DirectExecutionRepositoryStreet : CrudRepository<DirectExecutionStreet, Long> {
}

@Repository
interface DirectExecutionRepositoryStreetItem : CrudRepository<DirectExecutionStreetItem, Long> {
}

@Repository
interface DirectExecutionExecutorRepository : CrudRepository<DirectExecutionExecutor, Long> {
}