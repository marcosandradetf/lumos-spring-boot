package com.lumos.lumosspring.execution.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("direct_execution_street_item")
data class DirectExecutionStreetItem (
    @Id
    var directExecutionStreetItemId: Long? = null,
    var measuredItemQuantity: Double = 0.0,
    var contractItemId: Long,
    var directExecutionStreetId: Long
)