package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.PContractReferenceItemDTO
import com.lumos.lumosspring.contract.service.ContractService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ContractController(
    private val contractService: ContractService
) {

    @GetMapping("/contracts/get-items")
    fun getItems() : ResponseEntity<Any> {
        return contractService.getReferenceItems()
    }

    @PostMapping("/contracts/insert-contract")
    fun insertContract(@RequestBody contractDTO: ContractDTO) : ResponseEntity<Any> =
        contractService.saveContract(contractDTO)

    @PostMapping("/contracts/delete-by-id")
    fun deleteById(@RequestBody contractId: Long) : ResponseEntity<Any> =
        contractService.deleteById(contractId)


    @GetMapping("/contracts/get-contract/{contractId}")
    fun getContract(@PathVariable contractId: Long): ResponseEntity<Any> =
         contractService.getContract(contractId)

    @GetMapping("/contracts/get-AllContracts")
    fun getAllActiveContracts(): ResponseEntity<Any> =
        contractService.getAllActiveContracts()

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
    fun getItemsForMob(): ResponseEntity<MutableList<PContractReferenceItemDTO>> {
        return contractService.getItemsForMob()
    }

}