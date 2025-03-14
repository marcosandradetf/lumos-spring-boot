package com.lumos.lumosspring.contract.dto

data class ContractDTO(
    val number: String,
    val socialReason: String,
    var cnpj : String,
    var address : String,
    var phone : String,
    var items : List<ContractItemsDTO>
)
