package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.stock.controller.dto.DepositDTO;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.service.DepositService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposit")
public class DepositController {
    @Autowired
    private DepositService almoxarifadoService;

    @GetMapping
    public List<Deposit> getAll() {

        return almoxarifadoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Deposit> getById(@PathVariable Long id) {
        Deposit almoxarifado = almoxarifadoService.findById(id);
        return almoxarifado != null ? ResponseEntity.ok(almoxarifado) : ResponseEntity.notFound().build();
    }

    @PostMapping("/insert")
    public ResponseEntity<?> create(@RequestBody DepositDTO almoxarifado) {
        return almoxarifadoService.save(almoxarifado);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DepositDTO almoxarifado) {
        return almoxarifadoService.update(id, almoxarifado);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return almoxarifadoService.delete(id);
    }
}
