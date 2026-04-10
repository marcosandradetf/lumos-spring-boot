package com.lumos.lumosspring.contract.service

import com.google.api.client.util.Data.mapOf
import com.lumos.lumosspring.contract.dto.ContractItemBalance
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class ContractViewService(
    private val contractRepository: ContractRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
) {
    fun getContractItemBalance(contractId: Long): ResponseEntity<List<ContractItemBalance>> {
        return ResponseEntity.ok(contractRepository.getContractItemBalance(contractId))
    }

    fun getReferenceItems(): ResponseEntity<Any> {
        val referenceItems = contractReferenceItemRepository.findAllByTenantId(Utils.getCurrentTenantId())
        val referenceItemsResponse: MutableList<ContractReferenceItemDTO> = mutableListOf()

        for (item in referenceItems.sortedBy { it.description }) {
            referenceItemsResponse.add(
                ContractReferenceItemDTO(
                    item.contractReferenceItemId!!,
                    item.description,
                    item.nameForImport,
                    item.type,
                    item.linking,
                    item.itemDependency,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
                )
            )
        }

        return ResponseEntity.ok().body(referenceItemsResponse)
    }

    fun hasContractActive(ibgeCode: String, appClient: String, appVersion: String): ResponseEntity<Any> {
        if (appClient != "lumos-web" || appVersion != "1.0.0") {
            throw Utils.BusinessException("Client not allowed or API version not supported")
        }

        val contract: Contract? = contractRepository.findContractByIbgeCodeAndContractTypeInAndDueDateAfter(
            ibgeCode,
            listOf("ALL", "MAINTENANCE"),
            Instant.now()
        ).orElse(null)

        return ResponseEntity.ok(
            mapOf(
                "hasValidContract" to (contract != null),
                "contractId" to contract?.contractId
            )
        )
    }
}
