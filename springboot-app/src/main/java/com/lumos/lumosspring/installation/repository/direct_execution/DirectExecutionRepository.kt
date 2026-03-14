package com.lumos.lumosspring.installation.repository.direct_execution

import com.lumos.lumosspring.contract.dto.ItemResponseDTO
import com.lumos.lumosspring.installation.model.direct_execution.*
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


@Repository
interface DirectExecutionRepository : CrudRepository<DirectExecution, Long> {
    data class InstallationRow(val status: String, val description: String)

    @Query(
        """
        SELECT direct_execution_status as status, description
        FROM direct_execution 
        WHERE direct_execution_id = :id
    """
    )
    fun getInstallation(id: Long): InstallationRow?

    @Modifying
    @Query(
        """
        UPDATE direct_execution 
        SET direct_execution_status = :status,
            responsible = :responsible,
            signature_uri = :signatureUri,
            sign_date = :signDate,
            finished_at = :finishedAt
        WHERE direct_execution_id = :id
    """
    )
    fun finishDirectExecution(
        @Param("id") id: Long,
        @Param("status") status: String,
        @Param("signatureUri") signatureUri: String?,
        @Param("signDate") signDate: Instant?,
        @Param("finishedAt") finishedAt: Instant,
        @Param("responsible") responsible: String?
    )

    @Modifying
    @Query(
        """
            update direct_execution
            set report_view_at = now()
            where direct_execution_id in (:ids) and report_view_at is null
        """
    )
    fun registerGeneration(ids: List<Long>)


    @Modifying
    @Query("""
        UPDATE direct_execution
        SET team_id = :teamId
        WHERE reservation_management_id = :id
    """)
    fun updateTeamId(reservationManagementId: Long, teamId: Long)
    fun existsDirectExecutionByExternalIdAndTenantId(externalId: Long, tenantId: UUID): Boolean

    @Query("""
        WITH updated AS (
            UPDATE direct_execution
            SET direct_execution_status = 'VALIDATING'
            WHERE external_id = :externalId
              AND direct_execution_status <> 'VALIDATING'
            RETURNING direct_execution_id
        )
        SELECT direct_execution_id FROM updated
        UNION ALL
        SELECT direct_execution_id 
        FROM direct_execution
        WHERE external_id = :externalId
          AND NOT EXISTS (SELECT 1 FROM updated)
    """)
    fun findDirectExecutionIdByExternalId(externalId: Long): Long?

    fun findAllByDirectExecutionStatusAndTenantId(
        directExecutionStatus: String,
        tenantId: UUID
    ): MutableList<DirectExecution>

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

    @Query("""
        select
            ci.contract_item_id,
            cri.description,
            dei.measured_item_quantity as quantity,
            cri.type,
            cri.linking,
            ci.contracted_quantity - ci.quantity_executed as current_balance,
            cri.contract_reference_item_id,
            cri.truck_stock_control
        from direct_execution_item dei
        inner join contract_item ci on ci.contract_item_id = dei.contract_item_id
        inner join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
        where dei.direct_execution_id = :directExecutionId
            and dei.item_status = :itemStatus
            and cri.type not in ('SERVIÇO', 'PROJETO', 'MANUTENÇÃO','EXTENSÃO DE REDE', 'TERCEIROS', 'CEMIG')
        order by description
    """)
    fun getItemsByDirectExecutionId(directExecutionId: Long, itemStatus: String): List<ItemResponseDTO>

    @Modifying
    @Query("""
        delete from direct_execution_item
        where direct_execution_id = :installationId
    """)
    fun deleteByDirectExecutionId(installationId: Long)


}

@Repository
interface DirectExecutionRepositoryStreet : CrudRepository<DirectExecutionStreet, Long> {
    @Query(
        """
        select direct_execution_street_id
        from direct_execution_street
        where direct_execution_id = :id
    """
    )
    fun getByDirectExecutionId(id: Long): List<Long>
    fun existsDirectExecutionStreetByDeviceStreetIdAndDeviceId(deviceStreetId: Long, deviceId: String): Boolean
    fun findAllByDirectExecutionId(directExecutionId: Long): MutableList<DirectExecutionStreet>
}

@Repository
interface DirectExecutionRepositoryStreetItem : CrudRepository<DirectExecutionStreetItem, Long> {
    fun findAllByDirectExecutionStreetIdIn(directExecutionStreetIds: MutableCollection<Long>): MutableList<DirectExecutionStreetItem>
    fun findByDirectExecutionStreetItemIdIn(directExecutionStreetItemIds: MutableCollection<Long>): MutableList<DirectExecutionStreetItem>
}

@Repository
interface DirectExecutionExecutorRepository : CrudRepository<DirectExecutionExecutor, Long> {
}