package com.lumos.lumosspring.maintenance.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable

@Table
data class Maintenance(
    @Id
    val maintenanceId: UUID,
    val contractId: Long?,
    val pendingPoints: Boolean,
    val quantityPendingPoints: Int?,
    val dateOfVisit: Instant,
    val type: String, //rural ou urbana
    val status: String,

    val signDate: Instant? = null,
    val responsible: String? = null,
    val signatureUri: String? = null,

    @Transient
    private var isNewEntry: Boolean = true
) : Persistable<UUID> {
    override fun getId(): UUID = maintenanceId
    override fun isNew(): Boolean = isNewEntry
}
