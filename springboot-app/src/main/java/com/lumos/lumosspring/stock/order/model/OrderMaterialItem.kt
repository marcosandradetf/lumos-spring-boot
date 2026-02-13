package com.lumos.lumosspring.stock.order.model

import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.annotation.Id
import java.util.UUID
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.math.BigDecimal

@Table
data class OrderMaterialItem(
    @Id
    val orderId: UUID,
    val materialId: Long,
    val status: String = ReservationStatus.PENDING,
    val quantityReleased: BigDecimal? = null,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID> {
    override fun getId(): UUID = orderId
    override fun isNew(): Boolean = isNewEntry
}