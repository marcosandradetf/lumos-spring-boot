package com.lumos.lumosspring.stock.entities

import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Transient


@Table
data class OrderMaterial(
    @Id
    val orderId: UUID,
    val orderCode: String,
    val createdAt: Instant,
    val depositId: Long,
    val status: String,
    val teamId: Long,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<UUID> {
    override fun getId(): UUID = orderId
    override fun isNew(): Boolean = isNewEntry
}
