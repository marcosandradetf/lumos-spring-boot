package com.lumos.lumosspring.contract.dto

import java.time.Instant

data class ContractDTO(
    val contractId: Long?,
    val number: String,
    val contractor: String,
    var cnpj : String,
    var address : String,
    var phone : String,
    var unifyServices : Boolean,
    var noticeFile : String?,
    var contractFile : String?,
    var items : List<ContractReferenceItemDTO>,
    val companyId: Long,

    var ibgeCode: String,
    var contractionDate: Instant,
    var dueDate: Instant,
    var contractType: String,
)
