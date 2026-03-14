package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.contract.service.ContractService.ExecutedQuantity
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
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

    var contractedQuantity : BigDecimal = BigDecimal.ZERO
    var quantityExecuted: BigDecimal = BigDecimal.ZERO

    var unitPrice : BigDecimal = BigDecimal.ZERO
    var totalPrice : BigDecimal = BigDecimal.ZERO

    @Transient
    var referenceItem: ContractReferenceItem? = null

    @Transient
    var executedQuantity: List<ExecutedQuantity> = emptyList()

    @Transient
    var reservedQuantity: List<ExecutedQuantity> = emptyList()

    @Version
    var version: Long? = null

    fun setPrices(itemValue: BigDecimal) {
        unitPrice = itemValue
        totalPrice = itemValue.multiply(contractedQuantity)
        // Não chame métodos em outras entidades aqui
    }
}
