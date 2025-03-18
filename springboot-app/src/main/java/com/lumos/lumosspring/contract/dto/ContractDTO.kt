package com.lumos.lumosspring.contract.dto

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
    var items : List<ContractItemsDTO>
)
