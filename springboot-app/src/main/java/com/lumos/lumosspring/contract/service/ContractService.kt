package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.dto.PContractReferenceItemDTO
import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.contract.entities.ContractItem
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.notifications.service.Routes
import com.lumos.lumosspring.user.Role
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.ContractStatus
import com.lumos.lumosspring.util.DefaultResponse
import com.lumos.lumosspring.util.JdbcUtil
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.Util
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class ContractService(
    private val contractRepository: ContractRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val util: Util,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val namedJdbc: NamedParameterJdbcTemplate
) {

    fun getReferenceItems(): ResponseEntity<Any> {
        val referenceItems = contractReferenceItemRepository.findAll()
        val referenceItemsResponse: MutableList<ContractReferenceItemDTO> = mutableListOf()

        for (item in referenceItems.sortedBy { it.contractReferenceItemId }) {
            referenceItemsResponse.add(
                ContractReferenceItemDTO(
                    item.contractReferenceItemId,
                    item.description,
                    item.nameForImport,
                    item.type,
                    item.linking,
                    item.itemDependency,
                    0.0,
                    "0,00"
                )
            )
        }

        return ResponseEntity.ok().body(referenceItemsResponse)
    }

    fun deleteById(contractId: Long): ResponseEntity<Any> {
        try {
            contractRepository.deleteById(contractId);
        } catch (e: Exception) {
            throw IllegalStateException("Como este contrato já possui registros vinculados (como reservas, medições ou execuções), a exclusão não é permitida.")
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    fun saveContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        val contract = Contract()
        contract.contractNumber = contractDTO.number
        contract.contractor = contractDTO.contractor
        contract.cnpj = contractDTO.cnpj
        contract.address = contractDTO.address
        contract.creationDate = util.dateTime
        contract.createdBy = userRepository.findByUserId(UUID.fromString(contractDTO.userUUID)).orElseThrow()
        contract.unifyServices = contractDTO.unifyServices
        contract.noticeFile = if ((contractDTO.noticeFile?.length ?: 0) > 0) contractDTO.noticeFile else null
        contract.contractFile = if ((contractDTO.contractFile?.length ?: 0) > 0) contractDTO.contractFile else null

        contractRepository.save(contract)


        contractDTO.items.forEach { item ->
            val ci = ContractItem()
            val reference = contractReferenceItemRepository.findById(item.contractReferenceItemId)
            if (!reference.isPresent) {
                throw RuntimeException("Item ${item.contractReferenceItemId} not found")
            }
            ci.referenceItem = reference.get()
            ci.contract = contract
            ci.contractedQuantity = item.quantity!!
            ci.setPrices(util.convertToBigDecimal(item.price)!!)
            contractItemsQuantitativeRepository.save(ci)
        }

        notificationService.sendNotificationForRole(
            title = "Novo contrato pendente para Pré-Medição",
            body = "Colaboradora ${contract.createdBy.name} criou o contrato de ${contract.contractor}",
            action = Routes.CONTRACT_SCREEN,
            role = Role.Values.RESPONSAVEL_TECNICO,
            time = util.dateTime,
            type = NotificationType.CONTRACT
        )

        return ResponseEntity.ok(DefaultResponse("Contrato salvo com sucesso!"))
    }

    fun getContract(contractId: Long): ResponseEntity<Any> {
        data class ItemsForReport(
            val number: Int,
            val contractItemId: Long,
            val description: String,
            val unitPrice: BigDecimal,
            val contractedQuantity: Double,
            val linking: String?,
        )

        data class ContractForReport(
            val contractId: Long,
            val number: String,
            val contractor: String,
            val cnpj: String,
            val phone: String,
            val address: String,
            val contractFile: String?,
            val createdBy: String,
            val createdAt: String,
            val items: List<ItemsForReport>,
        )

        val items = mutableListOf<ItemsForReport>()

        val contract = contractRepository.findContractByContractId(contractId).orElseThrow()
        var number = 1
        contract.contractItem.sortedBy { it.referenceItem.description }
            .forEach { item ->
                items.add(
                    ItemsForReport(
                        number = number,
                        contractItemId = item.contractItemId,
                        description = item.referenceItem.description,
                        unitPrice = item.unitPrice,
                        contractedQuantity = item.contractedQuantity,
                        linking = item.referenceItem.linking,
                    )
                )
                number += 1
            }

        return ResponseEntity.ok(
            ContractForReport(
                contractId = contract.contractId,
                number = contract.contractNumber ?: "",
                contractor = contract.contractor ?: "",
                cnpj = contract.cnpj ?: "",
                phone = contract.phone ?: "",
                address = contract.address ?: "",
                contractFile = contract.contractFile,
                createdBy = contract.createdBy.completedName,
                createdAt = contract.creationDate.toString(),
                items = items
            )
        )
    }

    fun getAllActiveContracts(): ResponseEntity<Any> {
        data class ContractResponseDTO(
            val contractId: Long,
            val number: String,
            val contractor: String,
            val address: String,
            val phone: String,
            val cnpj: String,
            val noticeFile: String,
            val contractFile: String,
            val createdBy: String,
            val itemQuantity: Int,
            val contractStatus: String,
            val contractValue: String,
            val additiveFile: String,
        )

        return ResponseEntity.ok().body(contractRepository.findAllByStatus(ContractStatus.ACTIVE).map {
            ContractResponseDTO(
                contractId = it.contractId,
                number = it.contractNumber ?: "",
                contractor = it.contractor ?: "",
                address = it.address ?: "",
                phone = it.phone ?: "",
                cnpj = it.cnpj ?: "",
                noticeFile = it.noticeFile ?: "",
                contractFile = it.contractFile ?: "",
                itemQuantity = it.contractItem.size,
                createdBy = it.createdBy.completedName,
                contractStatus = it.status,
                contractValue = it.contractValue.toString(),
                additiveFile = ""
            )
        })
    }

    fun getContractItems(contractId: Long): ResponseEntity<Any> {
        data class ContractItemsResponse(
            val number: Int,
            val contractItemId: Long,
            val description: String,
            val unitPrice: String,
            val contractedQuantity: Double,
            val executedQuantity: Double,
            val linking: String?,
            val nameForImport: String?,
            val type: String
        )

        return ResponseEntity.ok().body(
            contractRepository.findById(contractId).orElseThrow()
                .contractItem
                .sortedBy { it.referenceItem.description }
                .mapIndexed { index, it ->
                    ContractItemsResponse(
                        number = index + 1,
                        contractItemId = it.contractItemId,
                        description = it.referenceItem.description,
                        unitPrice = it.unitPrice.toPlainString(),
                        contractedQuantity = it.contractedQuantity,
                        executedQuantity = it.quantityExecuted,
                        linking = it.referenceItem.linking,
                        nameForImport = it.referenceItem.nameForImport,
                        type = it.referenceItem.type
                    )
                })

    }

    @Cacheable("GetContractsForPreMeasurement")
    fun getContractsForPreMeasurement(): ResponseEntity<Any> {
        data class ContractForPreMeasurementDTO(
            val contractId: Long,
            val contractor: String,
            val contractFile: String?,
            val createdBy: String,
            val createdAt: String,
            val status: String,
            val itemsIds: String? = null,
        )

        val notAllowedTypes = listOf("SERVIÇO", "PROJETO", "CABO", "RELÉ")
        val contractList = contractRepository.findAllByStatus(ContractStatus.ACTIVE).map { contract ->

            val filteredIds = contract.contractItem
                .filter { it.referenceItem.type.trim().uppercase() !in notAllowedTypes }
                .map { it.referenceItem.contractReferenceItemId }
                .joinToString("#")

            ContractForPreMeasurementDTO(
                contractId = contract.contractId,
                contractor = contract.contractor!!,
                contractFile = contract.contractFile,
                createdBy = contract.createdBy.name,
                createdAt = contract.creationDate.toString(),
                status = contract.status,
                itemsIds = filteredIds.ifBlank { null }
            )
        }

        return ResponseEntity.ok(contractList)
    }


    @Cacheable("GetItemsForMobPreMeasurement")
    fun getItemsForMob(): ResponseEntity<MutableList<PContractReferenceItemDTO>> {
        val items = contractReferenceItemRepository.findAllByPreMeasurement()

        items.sortWith(
            compareBy<PContractReferenceItemDTO> { it.description }
                .thenBy { util.extractNumber(it.linking) }
        )

        return ResponseEntity.ok(items)
    }

    fun getContractItemsWithExecutionsSteps(contractId: Long): ResponseEntity<Any> {
        data class ContractItemsResponseWithExecutions(
            val number: Int,
            val contractItemId: Long,
            val description: String,
            val unitPrice: String,
            val contractedQuantity: Double,
            val executedQuantity: List<ExecutedQuantity>,
            val totalExecuted: Double,
            val linking: String?,
            val nameForImport: String?,
            val type: String
        )

        return ResponseEntity.ok().body(
            contractRepository.findById(contractId).orElseThrow()
                .contractItem
                .sortedWith(
                    compareByDescending<ContractItem> { it.quantityExecuted }
                        .thenBy { it.referenceItem.description }
                )
                .mapIndexed { index, it ->
                    ContractItemsResponseWithExecutions(
                        number = index + 1,
                        contractItemId = it.contractItemId,
                        description = it.referenceItem.description,
                        unitPrice = it.unitPrice.toPlainString(),
                        contractedQuantity = it.contractedQuantity,
                        executedQuantity = getExecutedQuantityByContract(it.contractItemId),
                        totalExecuted = it.quantityExecuted,
                        linking = it.referenceItem.linking,
                        nameForImport = it.referenceItem.nameForImport,
                        type = it.referenceItem.type
                    )
                })

    }

    data class ExecutedQuantity(
        val directExecutionId: Long,
        val step: Int,
        val quantity: Double,
    )

    fun getExecutedQuantityByContract(contractItemId: Long): List<ExecutedQuantity> {
        val directExecutions = JdbcUtil.getRawData(
            namedJdbc,
            """
                SELECT des.direct_execution_id, sum(desi.executed_quantity) as executed_quantity
                from direct_execution_street des
                inner join direct_execution_street_item desi on desi.direct_execution_street_id = des.direct_execution_street_id
                where contract_item_id = :contractItemId
                group by des.direct_execution_id
                order by des.direct_execution_id 
            """.trimIndent(),
            mapOf("contractItemId" to contractItemId)
        )

        return  directExecutions.mapIndexed { index, row ->
            ExecutedQuantity(
                directExecutionId = row["direct_execution_id"] as Long,
                step = index + 1,
                quantity = (row["executed_quantity"] as Number).toDouble(),
            )
        }
    }

}
