package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.user.AppUser
import com.lumos.lumosspring.util.ContractStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractId: Long = 0
    var contractNumber: String? = null
    var contractor : String? = null
    var cnpj : String? = null
    var address : String? = null
    var phone : String? = null
    var creationDate : Instant = Instant.now()
    @ManyToOne
    @JoinColumn(name = "created_by_id_user")
    var createdBy : AppUser = AppUser()
    var contractValue : BigDecimal = BigDecimal.ZERO;
    var unifyServices : Boolean = false
    var noticeFile : String? = null
    var contractFile : String? = null
    var status : String = ContractStatus.ACTIVE

    @OneToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true, mappedBy = "contract")
    var contractItem: Set<ContractItem> = hashSetOf()

    fun sumTotalPrice(totalPrice: BigDecimal?) {
        this.contractValue = this.contractValue.add(totalPrice)
    }
}