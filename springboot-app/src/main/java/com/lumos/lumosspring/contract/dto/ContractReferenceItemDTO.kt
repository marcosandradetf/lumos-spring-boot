package com.lumos.lumosspring.contract.dto


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

data class PContractReferenceItemDTO(
    var contractReferenceItemId: Long,
    var description : String,
    var nameForImport : String,
    var type : String?,
    var linking : String?,
    var itemDependency : String?,
)
