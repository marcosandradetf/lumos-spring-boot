package com.lumos.lumosspring.stock.order.teamrequest.model

import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.annotation.Id
import java.util.UUID
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable


@Table
data class OrderMaterialItem(
    @Id
    val orderId: UUID,
    val materialId: Long,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID> {
    override fun getId(): UUID = orderId
    override fun isNew(): Boolean = isNewEntry
}

