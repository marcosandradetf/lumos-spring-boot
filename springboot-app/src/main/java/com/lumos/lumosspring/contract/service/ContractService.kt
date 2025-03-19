package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.stock.repository.MaterialServiceRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.DefaultResponse
import com.lumos.lumosspring.util.Util
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class ContractService(
    private val contractRepository: ContractRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val materialServiceRepository: MaterialServiceRepository,
    private val util: Util,
    private val userRepository: UserRepository
) {

    fun getReferenceItems(): ResponseEntity<Any> {
        val referenceItems = contractReferenceItemRepository.findAll()
        val referenceItemsResponse: MutableList<ContractReferenceItemDTO> = mutableListOf()

        for (item in referenceItems.sortedBy { it.contractReferenceItemId }) {
            referenceItemsResponse.add(ContractReferenceItemDTO(
                item.contractReferenceItemId,
                item.description,
                item.completeDescription,
                item.type,
                item.linking,
                item.itemDependency,
                0.0,
                "0,00"
            ))
        }

        return ResponseEntity.ok().body(referenceItemsResponse)
    }

    fun saveContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        val contract = Contract()
        contract.contractNumber = contractDTO.number
        contract.contractor = contractDTO.contractor
        contract.cnpj = contractDTO.cnpj
        contract.address = contractDTO.address
        contract.creationDate = util.dateTime
        contract.createdBy = userRepository.findByIdOrNull(UUID.fromString(contractDTO.userUUID))
        contract.unifyServices = contractDTO.unifyServices
        contract.noticeFile = if ((contractDTO.noticeFile?.length ?: 0) > 0) contractDTO.noticeFile else null
        contract.contractFile = if ((contractDTO.contractFile?.length ?: 0) > 0) contractDTO.contractFile else null
        contractRepository.save(contract)

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

        return ResponseEntity.ok(DefaultResponse("Contrato salvo com sucesso!"))
    }

}
