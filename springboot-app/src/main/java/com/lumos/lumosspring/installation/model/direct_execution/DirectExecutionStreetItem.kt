package com.lumos.lumosspring.installation.model.direct_execution

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("direct_execution_street_item")
data class DirectExecutionStreetItem (
    @Id
    var directExecutionStreetItemId: Long? = null,
    var executedQuantity: BigDecimal = BigDecimal.ZERO,
    var materialStockId: Long?,
    var contractItemId: Long,
    var directExecutionStreetId: Long
)