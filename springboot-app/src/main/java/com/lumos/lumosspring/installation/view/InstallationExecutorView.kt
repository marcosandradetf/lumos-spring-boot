package com.lumos.lumosspring.installation.view

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("installation_executor_view")
data class InstallationExecutorView(

    @Id
    val installationId: Long,

    val installationType: String,
    val userId: UUID
)
