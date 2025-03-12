package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.entities.ContractItem
import com.lumos.lumosspring.contract.service.ContractServiceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/contracts")
class ContractController(
    private val contractService: ContractServiceService
) {

    @GetMapping("/get-items")
    fun getItems() : ResponseEntity<Any> {
        return contractService.getMaterials()
    }


}