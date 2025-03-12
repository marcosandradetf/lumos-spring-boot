package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.stock.entities.MaterialService
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.HashSet

@Entity
@Table(name = "tb_contracts_services")
class ContractService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractServiceId: Long = 0

    @ManyToOne
    @JoinColumn(name = "service_id")
    var materialService: MaterialService = MaterialService()

    @ManyToMany(mappedBy = "contractsServices", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var contractsItems: MutableSet<ContractItem> = HashSet()

    var contractedQuantity: Double = 0.0

    var unitPrice: BigDecimal = BigDecimal.ZERO;
    var totalPrice: BigDecimal = BigDecimal.ZERO;

    fun setPrices(itemValue: BigDecimal, contractItemId: Long) {
        unitPrice = itemValue
        totalPrice = itemValue.multiply(BigDecimal.valueOf(contractedQuantity))
        contractsItems.find { it.contractItemId == contractItemId }?.contract?.sumTotalPrice(totalPrice)
    }

}