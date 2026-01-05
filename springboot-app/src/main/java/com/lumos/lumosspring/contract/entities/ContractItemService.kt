package com.lumos.lumosspring.contract.entities

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table
data class ContractItemService(
    @Id
    val contractItemReferenceId: Long,
    val contractItemReferenceIdService: Long,
    val factor: BigDecimal = BigDecimal.ONE,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<Long> {
    override fun getId(): Long = contractItemReferenceIdService
    override fun isNew(): Boolean = isNewEntry
}