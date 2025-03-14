package com.lumos.lumosspring.contract.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "tb_contracts")
class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractId: Long = 0
    var contractNumber: String? = null
    var socialReason : String? = null
    var cnpj : String? = null
    var address : String? = null
    var phone : String? = null
    var creationDate : Instant? = null
    var contractValue : BigDecimal = BigDecimal.ZERO;

    @OneToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var contractItemQuantitatives: Set<ContractItemsQuantitative> = hashSetOf()

    fun sumTotalPrice(totalPrice: BigDecimal?) {
        this.contractValue = this.contractValue.add(totalPrice)
    }
}