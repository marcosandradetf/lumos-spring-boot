package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.service.ContractService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    suspend fun insertContract(@RequestBody contractDTO: ContractDTO) : ResponseEntity<Any> {
        return contractService.saveContract(contractDTO)
    }

    @GetMapping("/mobile/contracts/get-contracts")
    fun getContracts(): ResponseEntity<Any> {
        return contractService.getContractsForPreMeasurement()
    }


}