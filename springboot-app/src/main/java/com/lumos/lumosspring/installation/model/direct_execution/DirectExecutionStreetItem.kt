package com.lumos.lumosspring.installation.model.direct_execution

import com.lumos.lumosspring.stock.materialsku.model.Material
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import org.springframework.data.annotation.Transient

@Table("direct_execution_street_item")
data class DirectExecutionStreetItem (
    @Id
    var directExecutionStreetItemId: Long? = null,
    var executedQuantity: BigDecimal = BigDecimal.ZERO,
    var materialStockId: Long?,
    var contractItemId: Long?,
    var directExecutionStreetId: Long,


) {
    @Transient
    @get:Transient
    var materialId: Long? = null

    @Transient
    @get:Transient
    var material: Material? = null

}