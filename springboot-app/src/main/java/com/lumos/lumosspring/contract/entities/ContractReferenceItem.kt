package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.stock.entities.MaterialService
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.HashSet

@Entity
@Table(name = "tb_contract_reference_items")
class ContractReferenceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractReferenceItemId: Long = 0
    var description : String? = null
    @Column(columnDefinition = "TEXT")
    var completeDescription : String? = null
    var type : String? = null
    var linking : String? = null

    @OneToMany(mappedBy = "referenceItem", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var contractsItems: MutableSet<ContractItemsQuantitative> = HashSet()

}