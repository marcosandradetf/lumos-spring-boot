package com.lumos.lumosspring.maintenance.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table
data class MaintenanceStreet(
    @Id
    val maintenanceStreetId: UUID,
    val maintenanceId: UUID,
    var address: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    val comment: String?,
    val lastPower: String?,
    val lastSupply: String?,
    val currentSupply: String?,
    val reason: String?,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID> {
    override fun getId(): UUID = maintenanceStreetId
    override fun isNew(): Boolean = isNewEntry
}