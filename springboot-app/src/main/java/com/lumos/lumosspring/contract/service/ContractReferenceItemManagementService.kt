package com.lumos.lumosspring.contract.service

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

internal enum class MatchConfidence {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

internal data class MaterialMatchResult(
    val material: String,
    val score: Int,
    val confidence: MatchConfidence,
)

private val CONTRACT_MATCH_STOP_WORDS = setOf("de", "da", "do", "para", "com", "ate", "tipo", "padrao")
private const val ACTIVE_RELATIONSHIP_STATUS = "ACTIVE"
private const val PENDING_RELATIONSHIP_STATUS = "Pendente de Vinculo com Item"

private fun normalizeForComparison(text: String): String {
    // Mantem o texto em um formato previsivel para comparacao deterministica.
    // A regra aqui segue o requisito: trocar virgula por ponto e remover caracteres especiais.
    return text
        .replace(',', '.')
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun tokenize(text: String): List<String> {
    // Depois da normalizacao, a tokenizacao eh apenas por espaco.
    val normalized = normalizeForComparison(text)
    if (normalized.isBlank()) {
        return emptyList()
    }

    return normalized.split(" ")
}

internal fun cleanTokens(tokens: List<String>): List<String> {
    // Remove ruido da comparacao:
    // 1. espacos vazios
    // 2. stopwords irrelevantes
    // 3. tokens duplicados, para nao inflar o score
    return tokens
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { CONTRACT_MATCH_STOP_WORDS.contains(it) }
        .distinct()
}

private fun isNumericToken(token: String): Boolean = token.any(Char::isDigit)

internal fun weightedScore(item: String, material: String): Int {
    // A comparacao eh feita somente com a interseccao de tokens relevantes.
    // Token textual igual soma 1.
    // Token com numero soma 2, porque numero tende a ser um indicativo mais forte de match.
    val itemTokens = cleanTokens(tokenize(item)).toSet()
    val materialTokens = cleanTokens(tokenize(material)).toSet()

    return itemTokens.intersect(materialTokens).sumOf { token ->
        if (isNumericToken(token)) 2 else 1
    }
}

internal fun getConfidence(score: Int): MatchConfidence {
    return when {
        score >= 3 -> MatchConfidence.HIGH
        score == 2 -> MatchConfidence.MEDIUM
        score == 1 -> MatchConfidence.LOW
        else -> MatchConfidence.NONE
    }
}

internal fun findBestMaterialMatch(item: String, materials: List<String>): MaterialMatchResult? {
    // Gera o score de todos os materiais candidatos e devolve o melhor.
    // Em empate, ordena pelo nome do material para manter o resultado deterministico.
    return materials
        .asSequence()
        .map { material ->
            val score = weightedScore(item, material)
            MaterialMatchResult(
                material = material,
                score = score,
                confidence = getConfidence(score),
            )
        }
        .maxWithOrNull(
            compareBy<MaterialMatchResult> { it.score }
                .thenBy { it.material }
        )
}

internal fun shouldBlock(item: String, materials: List<String>, existingLinks: Map<String, String>): Boolean {
    val normalizedItem = normalizeForComparison(item)
    // Se o item ja possui algum vinculo explicito cadastrado, nao deve bloquear por similaridade.
    val hasExplicitLink = existingLinks.keys.any { normalizeForComparison(it) == normalizedItem }

    if (hasExplicitLink) {
        return false
    }

    // O bloqueio so acontece quando o melhor material encontrado tiver confianca HIGH.
    return findBestMaterialMatch(item, materials)?.confidence == MatchConfidence.HIGH
}

@Service
class ContractReferenceItemManagementService(
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val contractItemDependencyRepository: ContractItemDependencyRepository,
    private val materialContractReferenceItemRepository: MaterialContractReferenceItemRepository,
    private val materialReferenceRepository: MaterialReferenceRepository,
) {
    private val dependencyDrivenTypes = setOf("SERVIÇO", "SERVICO", "PROJETO")
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
        // Primeiro preservamos a regra antiga de pendencia por status do item.
        val hasItemsPending = contractReferenceItemRepository.existsContractReferenceItemByTenantIdAndStatusNot(
            tenantId,
            ACTIVE_RELATIONSHIP_STATUS
        )

        // Carrega todos os itens referenciais e todos os materiais genericos do tenant.
        // Sao esses materiais genericos que entram na deteccao de similaridade.
        val items = contractReferenceItemRepository.findAllByTenantId(tenantId)
        val materials = materialReferenceRepository.findAllByTenantIdAndIsGeneric(tenantId, true)
        val materialNames = materials.map { it.materialName }
        val itemIds = items.mapNotNull { it.contractReferenceItemId }

        // Busca os vinculos explicitos ja salvos entre item referencial e material.
        // Isso eh importante porque similaridade alta sem vinculo deve bloquear,
        // mas similaridade alta com vinculo explicito nao deve bloquear.
        val materialLinks = if (itemIds.isEmpty()) {
            emptyList()
        } else {
            materialContractReferenceItemRepository.findByContractReferenceItemIdIn(itemIds)
        }
        val materialLinksByItemId = materialLinks.groupBy { it.contractReferenceItemId }

        // Guarda quais materiais precisam aparecer como pendentes na interface.
        val pendingMaterialIds = mutableSetOf<Long>()
        var hasSimilarityPending = false

        items.forEach { item ->
            val itemId = item.contractReferenceItemId ?: return@forEach

            // Aqui so precisamos saber se o item ja tem algum vinculo explicito.
            // Se existir pelo menos um vinculo salvo, o shouldBlock devolve false.
            val existingLinks = if (materialLinksByItemId[itemId].isNullOrEmpty()) {
                emptyMap()
            } else {
                mapOf(item.description to "LINKED")
            }

            if (!shouldBlock(item.description, materialNames, existingLinks)) {
                return@forEach
            }

            // Encontrou pelo menos um item com match HIGH sem vinculo explicito.
            // Isso significa que a operacao deve continuar sinalizada como pendente.
            hasSimilarityPending = true

            materials.forEach { material ->
                // Marca todos os materiais que bateram com HIGH para aparecerem como pendentes.
                if (getConfidence(weightedScore(item.description, material.materialName)) == MatchConfidence.HIGH) {
                    material.idMaterial?.let(pendingMaterialIds::add)
                }
            }
        }

        materials.forEach { material ->
            val materialId = material.idMaterial ?: return@forEach

            // Reflete o resultado da comparacao diretamente no status do material,
            // para o usuario identificar na interface sem precisar abrir detalhes.
            val nextStatus = if (pendingMaterialIds.contains(materialId)) {
                PENDING_RELATIONSHIP_STATUS
            } else {
                ACTIVE_RELATIONSHIP_STATUS
            }

            if (material.relationshipStatus != nextStatus) {
                material.relationshipStatus = nextStatus
            }
        }

        // Salva novo status
        materialReferenceRepository.saveAll(materials)


        // "materialsPending" cobre tanto o que acabamos de detectar agora
        // quanto qualquer material que ja esteja salvo com status diferente de ACTIVE.
        val materialsPending = hasSimilarityPending ||
            materialReferenceRepository.existsMaterialByRelationshipStatusNot(ACTIVE_RELATIONSHIP_STATUS)
        val hasPending = hasItemsPending || materialsPending

        return mapOf(
            "hasItemsPending" to hasItemsPending,
            "HasMaterialsPending" to materialsPending,
            "hasPending" to hasPending
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

            entity.description = description.trim()
            entity.nameForImport = description.trim()
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

            val savedId = entity.contractReferenceItemId!!

            materialContractReferenceItemRepository.deleteByContractReferenceItemId(savedId)
            request.materialIds.distinct().forEach { materialId ->
                materialContractReferenceItemRepository.save(
                    MaterialContractReferenceItem(materialId, savedId, true)
                )
            }

            contractItemDependencyRepository.deleteByContractItemReferenceId(savedId)
            val resolvedDependencyIds = request.dependencyReferenceItemIds
                .distinct()
                .filter { it != savedId }

            resolvedDependencyIds.forEach { dependencyId ->
                contractItemDependencyRepository.save(
                    ContractItemDependency().apply {
                        contractItemReferenceId = savedId
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
