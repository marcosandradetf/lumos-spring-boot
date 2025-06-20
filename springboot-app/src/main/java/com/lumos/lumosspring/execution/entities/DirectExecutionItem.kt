package com.lumos.lumosspring.execution.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("direct_execution_item")
data class DirectExecutionItem (
    @Id
    var directExecutionItemId: Long = 0,
    var measuredItemQuantity: Double = 0.0,
    var contractItemId: Long,
    var directExecutionId: Long
)