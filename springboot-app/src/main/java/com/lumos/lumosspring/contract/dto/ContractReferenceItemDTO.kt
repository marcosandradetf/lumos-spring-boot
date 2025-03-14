package com.lumos.lumosspring.contract.dto

import jakarta.persistence.Column

data class ContractReferenceItemDTO(
    var contractReferenceItemId: Long,
    var description : String?,
    var completeDescription : String?,
    var type : String?,
    var linking : String?,
)
