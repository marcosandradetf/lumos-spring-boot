package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.authentication.repository.TenantRepository
import com.lumos.lumosspring.contract.dto.ContractReferenceItemBaseManagementDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDependencyLinkDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemManagementDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemMaterialLinkDTO
import com.lumos.lumosspring.contract.dto.SaveContractReferenceItemBaseDTO
import com.lumos.lumosspring.contract.dto.SaveContractReferenceItemLinksDTO
import com.lumos.lumosspring.contract.entities.ContractItemDependency
import com.lumos.lumosspring.contract.entities.ContractReferenceItem
import com.lumos.lumosspring.contract.entities.MaterialContractReferenceItem
import com.lumos.lumosspring.contract.repository.ContractItemDependencyRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.installation.repository.view.InstallationViewRepository
import com.lumos.lumosspring.stock.materialsku.repository.MaterialContractReferenceItemRepository
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository
import com.lumos.lumosspring.util.Utils
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.abs

@Service
class ContractReferenceItemManagementService(
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val contractItemDependencyRepository: ContractItemDependencyRepository,
    private val materialContractReferenceItemRepository: MaterialContractReferenceItemRepository,
    private val materialReferenceRepository: MaterialReferenceRepository,
    private val tenantRepository: TenantRepository,
    private val installationViewRepository: InstallationViewRepository,
) {
    val dependencyDrivenTypes = setOf("SERVIÇO", "SERVICO", "PROJETO")
    private val materialOptionalTypes = setOf("EXTENSÃO DE REDE", "EXTENSAO DE REDE", "MANUTENÇÃO", "MANUTENCAO")

    fun getReferenceItemsBaseManagement(): ResponseEntity<List<ContractReferenceItemBaseManagementDTO>> {
        val items = contractReferenceItemRepository.findAllByTenantId(Utils.getCurrentTenantId())
        return ResponseEntity.ok(buildBaseResponse(items))
    }

    fun getReferenceItemsLinkManagement(): ResponseEntity<List<ContractReferenceItemManagementDTO>> {
        val items = contractReferenceItemRepository.findAllByTenantId(Utils.getCurrentTenantId())
        return ResponseEntity.ok(buildResponse(items))
    }

    @Cacheable(
        value = ["CheckIfHasPendingLink"],
        key = "#tenantId"
    )
    @Transactional
    fun checkIfHasPendingLink(tenantId: UUID): Map<String, Boolean> {
        // Verifica pendencia por status do item.
        val hasItemsPending = contractReferenceItemRepository.existsContractReferenceItemByTenantIdAndStatusNot(
            tenantId,
            "ACTIVE"
        )

        return mapOf(
            "hasItemsPending" to hasItemsPending,
        )
    }

    @Transactional
    @CacheEvict(
        value = ["GetItemsForMobPreMeasurement", "CheckIfHasPendingLink"],
        key = "#tenantId"
    )
    fun saveReferenceItemsBase(
        requests: List<SaveContractReferenceItemBaseDTO>,
        tenantId: UUID
    ): ResponseEntity<List<ContractReferenceItemBaseManagementDTO>> {
        if (requests.isEmpty()) {
            return ResponseEntity.ok(emptyList())
        }

        val ids = requests.mapNotNull { it.contractReferenceItemId }.distinct()
        val existingItems = if (ids.isEmpty()) {
            emptyMap()
        } else {
            contractReferenceItemRepository.findByContractReferenceItemIdIn(ids)
                .associateBy { it.contractReferenceItemId }
        }
        val savedItems = mutableListOf<ContractReferenceItem>()

        requests.forEach { request ->
            val description = request.description.trim()
            val type = request.type?.trim()?.takeIf { it.isNotBlank() }
                ?: throw Utils.BusinessException("Tipo obrigatorio para o item referencial.")

            if (description.isBlank()) {
                throw Utils.BusinessException("Descricao obrigatoria para o item referencial.")
            }

            val entity = if (request.contractReferenceItemId != null) {
                existingItems[request.contractReferenceItemId]
                    ?: throw Utils.BusinessException("Item referencial nao encontrado para atualizacao.")
            } else {
                ContractReferenceItem().apply {
                    this.tenantId = tenantId
                }
            }

            val newDescription = description.trim()

            entity.description = newDescription
            entity.nameForImport = newDescription
            entity.type = type
            entity.status = when (entity.type) {
                "SERVIÇO", "PROJETO" -> "Pendente de Vinculo com Item"
                "BRAÇO" -> "Pendente de Vinculo com Cabo"

                else -> "ACTIVE"
            }

            val saved = contractReferenceItemRepository.save(entity)
            contractItemDependencyRepository.deleteByContractItemReferenceId(
                saved.contractReferenceItemId
                    ?: throw Utils.BusinessException("Não foi possível recuperar o id do item ao salvar")
            )
            materialContractReferenceItemRepository.deleteByContractReferenceItemId(saved.contractReferenceItemId)
            savedItems.add(saved)

        }

        val refreshed =
            contractReferenceItemRepository.findByContractReferenceItemIdIn(savedItems.mapNotNull { it.contractReferenceItemId })

        return ResponseEntity.ok(buildBaseResponse(refreshed))
    }

    @Transactional
    @CacheEvict(
        value = ["GetItemsForMobPreMeasurement", "CheckIfHasPendingLink"],
        key = "#tenantId"
    )
    fun saveReferenceItemLinks(
        requests: List<SaveContractReferenceItemLinksDTO>,
        tenantId: UUID
    ): ResponseEntity<List<ContractReferenceItemManagementDTO>> {
        if (requests.isEmpty()) {
            return ResponseEntity.ok(emptyList())
        }

        val ids = requests.map { it.contractReferenceItemId }.distinct()
        val existingItems = contractReferenceItemRepository.findByContractReferenceItemIdIn(ids)
            .associateBy { it.contractReferenceItemId }

        requests.forEach { request ->
            val entity = existingItems[request.contractReferenceItemId]
                ?: throw Utils.BusinessException("Item referencial nao encontrado para atualizacao de vinculos.")

            val contractReferenceItemId = entity.contractReferenceItemId!!

            materialContractReferenceItemRepository.deleteByContractReferenceItemId(contractReferenceItemId)
            request.materialIds.distinct().forEach { materialId ->
                materialContractReferenceItemRepository.save(
                    MaterialContractReferenceItem(materialId, contractReferenceItemId, true)
                )
            }

            contractItemDependencyRepository.deleteByContractItemReferenceId(contractReferenceItemId)
            val resolvedDependencyIds = request.dependencyReferenceItemIds
                .distinct()
                .filter { it != contractReferenceItemId }

            resolvedDependencyIds.forEach { dependencyId ->
                contractItemDependencyRepository.save(
                    ContractItemDependency().apply {
                        contractItemReferenceId = contractReferenceItemId
                        contractItemReferenceIdDependency = dependencyId
                        factor = BigDecimal.ONE
                        isNewEntry = true
                    }
                )
            }

            entity.status = resolveStatus(
                entity.description,
                type = entity.type,
                dependencyIds = resolvedDependencyIds,
            )

            contractReferenceItemRepository.save(entity)
        }

        val refreshed = contractReferenceItemRepository.findByContractReferenceItemIdIn(ids)

        return ResponseEntity.ok(buildResponse(refreshed))
    }

    private fun buildBaseResponse(items: List<ContractReferenceItem>): List<ContractReferenceItemBaseManagementDTO> {
        return items.sortedBy { it.description }.map { item ->
            ContractReferenceItemBaseManagementDTO(
                contractReferenceItemId = item.contractReferenceItemId,
                description = item.description,
                type = item.type,
                status = item.status,
            )
        }
    }

    private fun buildResponse(items: List<ContractReferenceItem>): List<ContractReferenceItemManagementDTO> {
        if (items.isEmpty()) {
            return emptyList()
        }

        val ids = items.mapNotNull { it.contractReferenceItemId }
        val materialRelations = materialContractReferenceItemRepository.findByContractReferenceItemIdIn(ids)
        val materialIds = materialRelations.map { it.materialId }.distinct()
        val materialsById = if (materialIds.isEmpty()) {
            emptyMap()
        } else {
            materialReferenceRepository.findByIdMaterialIn(materialIds).associateBy { it.idMaterial }
        }
        val materialRelationsByItem = materialRelations.groupBy { it.contractReferenceItemId }

        val dependencies = if (ids.isEmpty()) {
            emptyList()
        } else {
            contractItemDependencyRepository.findByContractItemReferenceIdIn(ids)
        }
        val dependencyIds = dependencies.map { it.contractItemReferenceIdDependency }.distinct()
        val dependencyItemsById = if (dependencyIds.isEmpty()) {
            emptyMap()
        } else {
            contractReferenceItemRepository.findByContractReferenceItemIdIn(dependencyIds)
                .associateBy { it.contractReferenceItemId }
        }
        val dependenciesByItem = dependencies.groupBy { it.contractItemReferenceId }

        return items.sortedBy { it.description }.map { item ->
            val itemId = item.contractReferenceItemId!!
            val materialLinks = materialRelationsByItem[itemId].orEmpty().mapNotNull { relation ->
                val material = materialsById[relation.materialId] ?: return@mapNotNull null
                ContractReferenceItemMaterialLinkDTO(
                    materialId = relation.materialId,
                    materialName = material.materialName,
                    description = item.description,
                )
            }

            val dependencyLinks = dependenciesByItem[itemId].orEmpty().mapNotNull { relation ->
                val dependency =
                    dependencyItemsById[relation.contractItemReferenceIdDependency] ?: return@mapNotNull null
                ContractReferenceItemDependencyLinkDTO(
                    contractReferenceItemId = dependency.contractReferenceItemId!!,
                    description = dependency.description,
                    type = dependency.type,
                )
            }

            ContractReferenceItemManagementDTO(
                contractReferenceItemId = itemId,
                description = item.description,
                type = item.type,
                status = resolveStatus(
                    item.description,
                    item.type,
                    dependencyLinks.map { it.contractReferenceItemId }),
                materialLinks = materialLinks.sortedBy { it.materialName },
                dependencyLinks = dependencyLinks.sortedBy { it.description },
            )
        }
    }

    private fun resolveStatus(description: String, type: String?, dependencyIds: List<Long>): String {
        val dependencyTypes = contractReferenceItemRepository.findAllById(dependencyIds)
            .map { it.type }
            .distinct()

        return when (type) {
            "SERVIÇO" -> {
                if(description.contains("LED") && dependencyTypes.contains("LED")) {
                    "ACTIVE"
                } else if(description.contains("BRAÇO") && dependencyTypes.contains("BRAÇO")) {
                    "ACTIVE"
                } else {
                    "Pendente de Vinculo com Item"
                }
            }

            "PROJETO" -> {
                if (dependencyTypes.contains("LED")) {
                    "ACTIVE"
                } else {
                    "Pendente de Vinculo com Item"
                }
            }

            "BRAÇO" -> {
                if (dependencyTypes.contains("CABO")) {
                    "ACTIVE"
                } else {
                    "Pendente de Vinculo com Cabo"
                }
            }

            else -> "ACTIVE"
        }
    }
}
