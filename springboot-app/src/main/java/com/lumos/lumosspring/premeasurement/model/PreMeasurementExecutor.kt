package com.lumos.lumosspring.premeasurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table
data class PreMeasurementExecutor(
    @Id
    val preMeasurementId: Long,
    val userId: UUID,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<Long> {
    override fun getId(): Long = preMeasurementId
    override fun isNew(): Boolean = isNewEntry
}
