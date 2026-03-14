package com.lumos.lumosspring.installation.model.direct_execution

import com.lumos.lumosspring.authentication.model.TenantEntity
import com.lumos.lumosspring.util.ExecutionStatus
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("direct_execution")
data class DirectExecution(
    @Id
    @Column("direct_execution_id")
    var directExecutionId: Long? = null,
    var description: String?,
    var instructions: String? = null,
    var contractId: Long? = null,
    var directExecutionStatus: String = ExecutionStatus.WAITING_STOCKIST,
    var teamId: Long,
    @Column("assigned_user_id")
    var assignedBy: UUID,
    var assignedAt: Instant,
    var reservationManagementId: Long? = null,
    var step: Int,
    var reportViewAt: Instant? = null,
    var availableAt: Instant? = null,
    var finishedAt: Instant? = null,
    var startedAt: Instant? = null,
    var externalId: Long? = null,

): TenantEntity(){

    @Transient
    var teamName: String? = null

    @Transient
    @get:Transient
    var streets: List<DirectExecutionStreet> = emptyList()
}
