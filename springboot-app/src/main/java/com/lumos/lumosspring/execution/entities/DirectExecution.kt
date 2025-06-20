package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.util.ExecutionStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("direct_execution")
data class DirectExecution(
    @Id
    var directExecutionId: Long = 0,
    var instructions: String? = null,
    var contractId: Long,
    var directExecutionStatus: String = ExecutionStatus.WAITING_STOCKIST,
    var teamId: Long,
    @Column("assigned_user_id")
    var assignedBy: UUID,
    var reservationManagementId: Long
)
