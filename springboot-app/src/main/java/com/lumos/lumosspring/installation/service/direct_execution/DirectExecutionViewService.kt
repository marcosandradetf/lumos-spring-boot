package com.lumos.lumosspring.installation.service.direct_execution

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.contract.service.ContractService
import com.lumos.lumosspring.installation.dto.direct_execution.DirectExecutionDTOResponse
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepository
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryStreet
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryStreetItem
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionViewRepository
import com.lumos.lumosspring.stock.materialsku.repository.MaterialContractReferenceItemRepository
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockViewRepository
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*


@Service
class DirectExecutionViewService(
    private val viewRepository: DirectExecutionViewRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val directExecutionRepositoryStreet: DirectExecutionRepositoryStreet,
    private val directExecutionRepositoryStreetItem: DirectExecutionRepositoryStreetItem,
    private val materialStockViewRepository: MaterialStockViewRepository,
    private val contractRepository: ContractRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val materialContractReferenceItemRepository: MaterialContractReferenceItemRepository,
    private val materialReferenceRepository: MaterialReferenceRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val teamRepository: TeamRepository,
    private val contractService: ContractService
) {

    fun getDirectExecutions(strUUID: String?): ResponseEntity<List<DirectExecutionDTOResponse>> {
        val userUUID = try {
            UUID.fromString(strUUID)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Usuário não encontrado")
        }

        val executions = viewRepository.getDirectExecutions(operatorUUID = userUUID)

        return ResponseEntity.ok().body(executions)
    }

    fun getDirectExecutionsV2(teamId: Long, status: String): ResponseEntity<List<DirectExecutionDTOResponse>> {
        val executions = viewRepository.getDirectExecutions(teamId = teamId, status = status)

        return ResponseEntity.ok().body(executions)
    }

    fun getGroupedInstallations(startDate: OffsetDateTime, endDate: OffsetDateTime): List<Map<String, JsonNode>> {
        return viewRepository.getGroupedInstallations(startDate, endDate)
    }

    fun getInstallationsWaitingValidation(): ResponseEntity<Any> {
        val executions = directExecutionRepository.findAllByDirectExecutionStatusAndTenantId(
            ExecutionStatus.AWAITING_COMPLETION,
            Utils.getCurrentTenantId()
        )

        val teamsMap = teamRepository.findByIdTeamIn(
            executions.map { it.teamId }.distinct()
        ).associateBy { it.idTeam }

        val executionsResponse = executions.map { execution ->
            val team = teamsMap[execution.teamId]
            execution.teamName = team?.teamName
            execution
        }

        return ResponseEntity.ok().body(executionsResponse)
    }

    fun getExecutionWaitingValidation(directExecutionId: Long): ResponseEntity<Any> {
        val execution = directExecutionRepository.findById(directExecutionId).orElseThrow()
        if(execution.directExecutionStatus != ExecutionStatus.AWAITING_COMPLETION) {
            throw Utils.BusinessException("Execução solicitada não está com status de validação")
        }

        val streets = directExecutionRepositoryStreet.findAllByDirectExecutionId(directExecutionId)
        val team = teamRepository.findById(execution.teamId).orElseThrow()
        val streetIds = streets.mapNotNull { it.directExecutionStreetId }

        val items = directExecutionRepositoryStreetItem
            .findAllByDirectExecutionStreetIdIn(streetIds.toMutableList())

        val itemsGroup = items.groupBy { it.directExecutionStreetId }

        val stockIds = items.mapNotNull { it.materialStockId }

        val stockMap = materialStockViewRepository
            .findByMaterialIdStockIn(stockIds)
            .associateBy { it.materialIdStock }

        val materialIds = stockMap.values.mapNotNull { it.materialId }.distinct()

        val materials = materialReferenceRepository
            .findByIdMaterialIn(materialIds)
            .associateBy { it.idMaterial }

        val referenceMap = materialContractReferenceItemRepository
            .findByMaterialIdIn(materialIds)
            .groupBy { it.materialId }

        val response = execution.copy().apply {
            this.teamName = team.teamName
            this.streets = streets.map { street ->
                // Criamos uma cópia da street para setar os itens processados
                street.copy().apply {
                    this.items = itemsGroup[this.directExecutionStreetId]?.map { item ->

                        val stock = stockMap[item.materialStockId]
                        val material = materials[stock?.materialId]

                        // Atualiza os referenceIds no objeto Material (que é Java/Mutable)
                        material?.referenceItemsIds = referenceMap[material.idMaterial]
                            ?.map { it.contractReferenceItemId }
                            ?: emptyList()

                        // O item.copy funciona porque materialId e material estão no CONSTRUTOR
                        item.copy().apply {
                            this.materialId = material?.idMaterial
                            this.material = material
                        }
                    } ?: emptyList()
                }
            }
        }

        return ResponseEntity.ok(response)
    }

    fun getContractItemsForLink(contractId: Long): ResponseEntity<Any> {
        val contract = contractRepository.findById(contractId).orElseThrow()
        val contractItems = contractItemsQuantitativeRepository.findByContractId(contractId)
        val contractReferenceItem = contractReferenceItemRepository.findByContractReferenceItemIdIn(
            contractItems.map { it.referenceItemId }
        ).associateBy { it.contractReferenceItemId }

        val referenceMaterialsIds = materialContractReferenceItemRepository.findByContractReferenceItemIdIn(
            contractItems.map { it.referenceItemId }
        ).groupBy { it.contractReferenceItemId }


        contractItems.forEach { contractItem ->
            val referenceItem = contractReferenceItem[contractItem.referenceItemId]
            referenceItem?.referenceMaterialsIds = referenceMaterialsIds[contractItem.referenceItemId]?.map {
                it.materialId
            } ?: emptyList()

            contractItem.referenceItemId = referenceItem?.contractReferenceItemId
            contractItem.referenceItem = referenceItem

            contractItem.executedQuantity =
                contractService.getExecutedQuantityByContract(
                    listOf(contractItem.contractItemId!!)
                )
            contractItem.reservedQuantity =
                contractService.getExecutedQuantityByContract(
                    listOf(contractItem.contractItemId!!),
                    true
                )
        }

        contract.items = contractItems

        return ResponseEntity.ok().body(contract)
    }


}
