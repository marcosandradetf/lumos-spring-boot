package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.service.ContractService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/contracts")
class ContractController(
    private val contractService: ContractService
) {

    @GetMapping("/get-items")
    fun getItems() : ResponseEntity<Any> {
        return contractService.getReferenceItems()
    }


}