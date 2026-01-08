package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.dto.ContractItemBalance
import com.lumos.lumosspring.contract.service.ContractViewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ContractViewController(
    private val service: ContractViewService
) {

    @GetMapping("/mobile/v1/contracts/get-contracts-balance/{contractId}")
    fun getContractItemBalance(@PathVariable contractId: Long): ResponseEntity<List<ContractItemBalance>> {
        return service.getContractItemBalance(contractId)
    }

    @GetMapping("/contracts/get-items")
    fun getItems() : ResponseEntity<Any> {
        return service.getReferenceItems()
    }

}