package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemBaseManagementDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemManagementDTO
import com.lumos.lumosspring.contract.dto.PContractReferenceItemDTO
import com.lumos.lumosspring.contract.dto.SaveContractReferenceItemBaseDTO
import com.lumos.lumosspring.contract.dto.SaveContractReferenceItemLinksDTO
import com.lumos.lumosspring.contract.service.ContractReferenceItemManagementService
import com.lumos.lumosspring.contract.service.ContractService
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api")
class ContractController(
    private val contractService: ContractService,
    private val contractReferenceItemManagementService: ContractReferenceItemManagementService,
) {
    @PostMapping("/contracts/insert-contract")
    @PreAuthorize("hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_ADMIN')")
    fun insertContract(@RequestBody contractDTO: ContractDTO): ResponseEntity<Any> =
        contractService.saveContract(contractDTO)

    @PutMapping("/contracts/update-items")
    fun changeItems(
        @RequestBody items: List<ContractReferenceItemDTO>,
        @RequestParam("contractId") contractId: Long
    ): ResponseEntity<Any> {
        return contractService.updateItems(contractId, items)
    }

    @GetMapping("/contracts/reference-items-management/base")
    fun getReferenceItemsBaseManagement(): ResponseEntity<List<ContractReferenceItemBaseManagementDTO>> {
        return contractReferenceItemManagementService.getReferenceItemsBaseManagement()
    }

    @PostMapping("/contracts/reference-items-management/base")
    fun saveReferenceItemsBase(
        @RequestBody items: List<SaveContractReferenceItemBaseDTO>
    ): ResponseEntity<List<ContractReferenceItemBaseManagementDTO>> {
        return contractReferenceItemManagementService.saveReferenceItemsBase(items, Utils.getCurrentTenantId())
    }

    @GetMapping("/contracts/reference-items-management/links")
    fun getReferenceItemsLinkManagement(): ResponseEntity<List<ContractReferenceItemManagementDTO>> {
        return contractReferenceItemManagementService.getReferenceItemsLinkManagement()
    }

    @PostMapping("/contracts/reference-items-management/links")
    fun saveReferenceItemLinks(
        @RequestBody items: List<SaveContractReferenceItemLinksDTO>
    ): ResponseEntity<List<ContractReferenceItemManagementDTO>> {
        return contractReferenceItemManagementService.saveReferenceItemLinks(items, Utils.getCurrentTenantId())
    }

    @PostMapping("/contracts/delete-by-id")
    fun deleteById(@RequestBody contractId: Long): ResponseEntity<Any> =
        contractService.deleteById(contractId)

    @PostMapping("/contracts/archive-by-id")
    fun archiveById(@RequestBody contractId: Long): ResponseEntity<Any> =
        contractService.archiveById(contractId)

    @GetMapping("/contracts/get-contract/{contractId}")
    fun getContract(@PathVariable contractId: Long): ResponseEntity<Any> =
        contractService.getContract(contractId)

    @PostMapping("/contracts/get-AllContracts")
    fun getAllActiveContracts(@RequestBody filters: FilterRequest): ResponseEntity<Any> =
        contractService.getAllActiveContracts(filters)

    data class FilterRequest(
        val contractor: String?,
        val startDate: Instant?,
        val endDate: Instant?,
        val status: String?
    )

    @GetMapping("/contracts/get-contract-items/{contractId}")
    fun getContractItems(@PathVariable contractId: Long): ResponseEntity<Any> =
        contractService.getContractItems(contractId)

    @GetMapping("/contracts/get-contract-items-with-executions-steps/{contractId}")
    fun getContractItemsWithExecutionsSteps(@PathVariable contractId: Long): ResponseEntity<Any> =
        contractService.getContractItemsWithExecutionsSteps(contractId)

    @GetMapping("/mobile/contracts/get-contracts")
    fun getContractsForPreMeasurement(): ResponseEntity<Any> {
        return contractService.getContractsForPreMeasurement()
    }

    @GetMapping("/mobile/contracts/get-reference-items")
    fun getItemsForMob(): ResponseEntity<List<PContractReferenceItemDTO>> {
        return contractService.getItemsForMob()
    }

    @GetMapping("/contracts/reference-items-management/check-if-pending-link")
    fun checkIfHasPendingLink(): Map<String, Boolean> {
        return contractReferenceItemManagementService.checkIfHasPendingLink(
            Utils.getCurrentTenantId()
        )
    }
}
