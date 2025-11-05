package com.lumos.lumosspring.stock.order.installationrequest.model

import com.lumos.lumosspring.authentication.model.TenantEntity
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Transient
import java.math.BigDecimal


@Table
data class MaterialHistory(
    @Id
    val materialHistoryId: UUID,
    val materialStockId: Long,

    val maintenanceStreetId: UUID? = null,
    val executionStreetId: Long? = null,

    val usedQuantity: BigDecimal,
    val usedDate: Instant,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID>, TenantEntity() {
    override fun getId(): UUID = materialHistoryId
    override fun isNew(): Boolean = isNewEntry
}
