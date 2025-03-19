package com.lumos.lumosspring.contract.entities

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "tb_contracts_items")
class ContractItemsQuantitative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractItemId : Long = 0

    @ManyToOne
    @JoinColumn(name = "contract_item_reference_id", nullable = false)
    var referenceItem : ContractReferenceItem = ContractReferenceItem()

    @ManyToOne
    var contract: Contract = Contract()

    var contractedQuantity : Double = 0.0
    private var unitPrice : BigDecimal = BigDecimal.ZERO;
    private var totalPrice : BigDecimal = BigDecimal.ZERO;

    fun setPrices(itemValue: BigDecimal) {
        unitPrice = itemValue
        totalPrice = itemValue.multiply(BigDecimal.valueOf(contractedQuantity))
        contract.sumTotalPrice(totalPrice)
    }


}