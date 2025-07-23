package com.lumos.lumosspring.contract.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
@Table("contract_item")
class ContractItem {
    @Id
    var contractItemId : Long? = null

    @Column("contract_item_reference_id")
    var referenceItemId: Long? = null

    @Column("contract_contract_id")
    var contractId: Long? = null

    var contractedQuantity : Double = 0.0
    var quantityExecuted: Double = 0.0

    var unitPrice : BigDecimal = BigDecimal.ZERO
    var totalPrice : BigDecimal = BigDecimal.ZERO

    fun setPrices(itemValue: BigDecimal) {
        unitPrice = itemValue
        totalPrice = itemValue.multiply(BigDecimal.valueOf(contractedQuantity))
        // Não chame métodos em outras entidades aqui
    }
}
