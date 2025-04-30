package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.dto.ContractDTO
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
    

    @GetMapping("/mobile/contracts/get-contracts")
    fun getContracts(): ResponseEntity<Any> {
        return contractService.getContractsForPreMeasurement()
    }

    @GetMapping("/contracts/get-contract/{contractId}")
    fun getContract(@PathVariable contractId: Long): ResponseEntity<Any> =
         contractService.getContract(contractId)

}