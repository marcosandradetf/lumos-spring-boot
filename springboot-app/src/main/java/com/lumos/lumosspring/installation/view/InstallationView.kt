package com.lumos.lumosspring.installation.view

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("installation_view")
data class InstallationView(
    @Id
    val installationId: Long,

    val installationType: String,
    val contractId: Long,
    val teamId: Long,
    val assignedUserId: UUID,
    val description: String?,
    val step: Int,
    val signatureUri: String?,
    val responsible: String?,
    val signDate: Instant?,
    val availableAt: Instant?,
    val finishedAt: Instant?,
    val startedAt: Instant?,
    val status: String,
    val reservationManagementId: Long?
)
