package com.lumos.lumosspring.contract.dto

data class ContractReferenceItemMaterialLinkDTO(
    val materialId: Long,
    val materialName: String,
    val description: String? = null,
)

data class ContractReferenceItemDependencyLinkDTO(
    val contractReferenceItemId: Long,
    val description: String,
    val type: String? = null,
)

data class ContractReferenceItemManagementDTO(
    val contractReferenceItemId: Long?,
    val description: String,
    val type: String?,
    val status: String,
    val materialLinks: List<ContractReferenceItemMaterialLinkDTO> = emptyList(),
    val dependencyLinks: List<ContractReferenceItemDependencyLinkDTO> = emptyList(),
)

data class ContractReferenceItemBaseManagementDTO(
    val contractReferenceItemId: Long?,
    val description: String,
    val type: String?,
    val status: String,
)

data class SaveContractReferenceItemBaseDTO(
    val clientDraftId: String? = null,
    val contractReferenceItemId: Long? = null,
    val description: String,
    val type: String?,
    val link: String? = null,
)

data class SaveContractReferenceItemLinksDTO(
    val contractReferenceItemId: Long,
    val materialIds: List<Long> = emptyList(),
    val dependencyReferenceItemIds: List<Long> = emptyList(),
)
