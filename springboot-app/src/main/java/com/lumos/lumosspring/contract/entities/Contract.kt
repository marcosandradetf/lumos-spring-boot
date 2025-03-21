package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.user.User
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
    var contractor : String? = null
    var cnpj : String? = null
    var address : String? = null
    var phone : String? = null
    var creationDate : Instant = Instant.now()
    @ManyToOne
    @JoinColumn(name = "created_by_id_user")
    var createdBy : User = User()
    private var contractValue : BigDecimal = BigDecimal.ZERO;
    var unifyServices : Boolean = false
    var noticeFile : String? = null
    var contractFile : String? = null
    var status : Status = Status.PRE_MEASUREMENT_PROGRESS

    @OneToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var contractItemsQuantitative: Set<ContractItemsQuantitative> = hashSetOf()

    fun sumTotalPrice(totalPrice: BigDecimal?) {
        this.contractValue = this.contractValue.add(totalPrice)
    }

    enum class Status {
        PRE_MEASUREMENT_PROGRESS,
        PRE_MEASUREMENT_FINISHED
    }
}