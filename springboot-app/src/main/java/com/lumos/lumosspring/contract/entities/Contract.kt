package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.authentication.model.TenantEntity
import com.lumos.lumosspring.util.ContractStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Table("contract")
class Contract : TenantEntity() {
    @Id
    var contractId: Long? = null
    var contractNumber: String? = null
    var contractor : String? = null
    var cnpj : String? = null
    var address : String? = null
    var phone : String? = null
    var creationDate : Instant = Instant.now()

    @Column("created_by_id_user")
    var createdBy: UUID? = null

    var unifyServices : Boolean = false
    var noticeFile : String? = null
    var contractFile : String? = null
    var status : String = ContractStatus.ACTIVE
    var companyId: Long = -1L
    var lastUpdatedBy: UUID? = null

}
