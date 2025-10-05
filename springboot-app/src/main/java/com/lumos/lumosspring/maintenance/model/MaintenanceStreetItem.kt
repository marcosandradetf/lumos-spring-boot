package com.lumos.lumosspring.maintenance.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.math.BigDecimal

@Table
data class MaintenanceStreetItem(
    @Id
    val maintenanceId: UUID,
    val maintenanceStreetId: UUID,
    val materialStockId: Long,
    val quantityExecuted: BigDecimal,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID> {
    override fun getId(): UUID = maintenanceId
    override fun isNew(): Boolean = isNewEntry
}
