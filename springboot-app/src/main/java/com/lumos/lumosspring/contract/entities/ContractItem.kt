package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.stock.entities.Material
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.HashSet

@Entity
@Table(name = "tb_contracts_items")
class ContractItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractItemId : Long = 0

    @ManyToOne
    @JoinColumn(name = "material_id")
    var material : Material = Material()

    @ManyToOne
    var contract: Contract = Contract()

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "tb_contracts_items_contracts_services",
        joinColumns = [JoinColumn(name = "contract_item_id")],
        inverseJoinColumns = [JoinColumn(name = "contract_service_id")]
    )
    var contractsServices: MutableSet<ContractService> = HashSet()

    var contractedQuantity : Double = 0.0

    var unitPrice : BigDecimal = BigDecimal.ZERO;
    var totalPrice : BigDecimal = BigDecimal.ZERO;

    fun setPrices(itemValue: BigDecimal) {
        unitPrice = itemValue
        totalPrice = itemValue.multiply(BigDecimal.valueOf(contractedQuantity))
        contract.sumTotalPrice(totalPrice)
    }

}