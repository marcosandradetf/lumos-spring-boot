package com.lumos.lumosspring.installation.model.direct_execution

import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("direct_execution_item")
data class DirectExecutionItem (
    @Id
    var directExecutionItemId: Long? = null,
    var measuredItemQuantity: BigDecimal = BigDecimal.ZERO,
    var contractItemId: Long,
    var directExecutionId: Long,
    var itemStatus: String = ReservationStatus.PENDING
)