package com.lumos.lumosspring.contract.controller

import com.lumos.lumosspring.contract.service.ContractRegisterService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/contract")
class ContractRegisterController(
    private val service: ContractRegisterService
) {
    @PreAuthorize("hasAuthority('SCOPE_SUPPORT')")
    @PostMapping("/validate")
    fun validateContract(
        @RequestParam(value = "approved", required = true) contractId: Long,
        @RequestParam(value = "approved", required = true) approved: Boolean,
        @RequestParam(value = "approved", required = false) reason: String?,
        @RequestParam(value = "approved", required = true) ibgeCode: String,
    ): ResponseEntity<Any> {
        return service.validateContract(
            contractId = contractId,
            approved = approved,
            reason = reason,
            ibgeCode = ibgeCode,
        )
    }

}