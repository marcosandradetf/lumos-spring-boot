package com.lumos.lumosspring.installation.model.direct_execution

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table
data class DirectExecutionExecutor(
    @Id
    val directExecutionId: Long,
    val userId: UUID,

    @Transient
    private var isNewEntry: Boolean = true
): Persistable<Long> {
    override fun getId(): Long = directExecutionId
    override fun isNew(): Boolean = isNewEntry
}
