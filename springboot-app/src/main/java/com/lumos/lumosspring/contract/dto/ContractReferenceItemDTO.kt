package com.lumos.lumosspring.contract.dto

import java.math.BigDecimal


data class ContractReferenceItemDTO(
    var contractReferenceItemId: Long,
    var description : String?,
    var nameForImport : String?,
    var type : String?,
    var linking : String?,
    var itemDependency : String?,
    var quantity : BigDecimal?,
    var price : String?,
    val contractItemId: Long? = null
)

data class PContractReferenceItemDTO(
    var contractReferenceItemId: Long,
    var description : String,
    var nameForImport : String,
    var type : String?,
    var linking : String?,
    var itemDependency : String?,
)
