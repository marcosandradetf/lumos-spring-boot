package com.lumos.lumosspring.contract.dto

import jakarta.persistence.Column

data class ContractReferenceItemDTO(
    var contractReferenceItemId: Long,
    var description : String?,
    var nameForImport : String?,
    var type : String?,
    var linking : String?,
    var itemDependency : String?,
    var quantity : Double?,
    var price : String?,
)
