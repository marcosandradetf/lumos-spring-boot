package com.lumos.lumosspring.maintenance.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table
data class MaintenanceExecutor(
    @Id
    val maintenanceId: UUID,
    val userId: UUID,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID> {
    override fun getId(): UUID = maintenanceId
    override fun isNew(): Boolean = isNewEntry
}
