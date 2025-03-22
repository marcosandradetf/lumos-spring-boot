package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.notification.service.NotificationService
import com.lumos.lumosspring.stock.repository.MaterialServiceRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.DefaultResponse
import com.lumos.lumosspring.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class ContractService(
    private val contractRepository: ContractRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val materialServiceRepository: MaterialServiceRepository,
    private val util: Util,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {

    suspend fun getReferenceItems(): ResponseEntity<Any> {
        val referenceItems = contractReferenceItemRepository.findAll()
        val referenceItemsResponse: MutableList<ContractReferenceItemDTO> = mutableListOf()

        for (item in referenceItems.sortedBy { it.contractReferenceItemId }) {
            referenceItemsResponse.add(
                ContractReferenceItemDTO(
                    item.contractReferenceItemId,
                    item.description,
                    item.completeDescription,
                    item.type,
                    item.linking,
                    item.itemDependency,
                    0.0,
                    "0,00"
                )
            )
        }

        val roleNames = setOf("ADMIN")
        val userIds: Optional<List<UUID>> = withContext(Dispatchers.IO) {
            userRepository.findAllByRoleNames(roleNames)
        }

        if (!userIds.isEmpty)
            notificationService.sendNotificationToMultipleUsersAsync(
                userIds = userIds.get(),
                title = "Novo Contrato disponível para pré-medição",
                body = "Contrato de ",
                action = "open_contracts"
            )


        return ResponseEntity.ok().body(referenceItemsResponse)
    }

    suspend fun saveContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        val contract = Contract()
        contract.contractNumber = contractDTO.number
        contract.contractor = contractDTO.contractor
        contract.cnpj = contractDTO.cnpj
        contract.address = contractDTO.address
        contract.creationDate = util.dateTime
        contract.createdBy = withContext(Dispatchers.IO) {
            userRepository.findByIdUser(UUID.fromString(contractDTO.userUUID))
        }.orElseThrow()
        contract.unifyServices = contractDTO.unifyServices
        contract.noticeFile = if ((contractDTO.noticeFile?.length ?: 0) > 0) contractDTO.noticeFile else null
        contract.contractFile = if ((contractDTO.contractFile?.length ?: 0) > 0) contractDTO.contractFile else null
        withContext(Dispatchers.IO) {
            contractRepository.save(contract)
        }

        contractDTO.items.forEach { item ->
            val ci = ContractItemsQuantitative()
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

//        val roleNames = setOf("RESPONSAVEL_TECNICO")
        val roleNames = setOf("ADMIN")
        val userIds: Optional<List<UUID>> = withContext(Dispatchers.IO) {
            userRepository.findAllByRoleNames(roleNames)
        }

        if (!userIds.isEmpty)
            notificationService.sendNotificationToMultipleUsersAsync(
                userIds = userIds.get(),
                title = "Novo Contrato disponível para pré-medição",
                body = "Contrato de ${contract.contractor} criado por ${contract.createdBy.name}",
                action = "open_contracts"
            )

        return ResponseEntity.ok(DefaultResponse("Contrato salvo com sucesso!"))
    }

    fun getContractsForPreMeasurement(): ResponseEntity<Any> {
        data class ContractForPreMeasurementDTO(
            val contractId: Long,
            val contractor: String,
            val contractFile: String?,
            val createdBy: String,
            val createdAt: String,
            val status: String,
        )

        val contractList = mutableListOf<ContractForPreMeasurementDTO>()

        contractRepository.findAll().forEach {
            val contract = ContractForPreMeasurementDTO(
                contractId = it.contractId,
                contractor = it.contractor!!,
                contractFile = it.contractFile,
                createdBy = it.createdBy.name,
                createdAt = it.creationDate.toString(),
                status = it.status.name
            )
            contractList.add(contract)
        }

        return ResponseEntity.ok(contractList)
    }

}
