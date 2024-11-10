package com.lumos.lumosspring.contrato.controller;

import com.lumos.lumosspring.contrato.controller.dto.ContratoRequest;
import com.lumos.lumosspring.contrato.service.ContratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contrato")
public class ContratoController {
    private final ContratoService contratoService;
    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @PostMapping
    public ResponseEntity<String> criarContrato(@RequestBody ContratoRequest contratoReq) {
        return contratoService.criarContrato(contratoReq);
    }

}
