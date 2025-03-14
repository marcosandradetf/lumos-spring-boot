package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.stock.repository.MaterialServiceRepository
import com.lumos.lumosspring.util.DefaultResponse
import com.lumos.lumosspring.util.Util
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ContractService(
    private val contractRepository: ContractRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val materialServiceRepository: MaterialServiceRepository,
    private val util: Util
) {

    fun getReferenceItems(): ResponseEntity<Any> {
        val referenceItems = contractReferenceItemRepository.findAll()
        val referenceItemsResponse: MutableList<ContractReferenceItemDTO> = mutableListOf()

        for (item in referenceItems) {
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

        return ResponseEntity.ok(DefaultResponse("Contrato salvo com sucesso!"))
    }

}
