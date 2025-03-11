package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.stock.entities.Material
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "tb_contracts_items")
class ContractItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractItemId : Long = 0

    var description : String? = null
    var type : String? = null
    var category : String? = null

    @ManyToMany(mappedBy = "contractItems")
    var contracts: Set<Contract> = hashSetOf()

    var itemQuantity : Double = 0.0

    var unitPrice : BigDecimal = BigDecimal.ZERO;
    var totalPrice : BigDecimal = BigDecimal.ZERO;

    fun setPrices(itemValue: BigDecimal, contractId : Long) {
        unitPrice = itemValue
        totalPrice = itemValue.multiply(BigDecimal.valueOf(itemQuantity))
        contracts.find { it.contractId == contractId }
            ?.sumTotalPrice(totalPrice)
    }

}