package com.lumos.lumosspring.contract.controller;

import com.lumos.lumosspring.contract.controller.dto.ContractRequest;
import com.lumos.lumosspring.contract.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contrato")
public class ContractController {
    private final ContractService contractService;
    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<String> createContract(@RequestBody ContractRequest contractReq) {
        return contractService.createContract(contractReq);
    }

}
