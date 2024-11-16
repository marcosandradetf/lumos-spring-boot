package com.lumos.lumosspring.stock.controller;

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

    @PostMapping
    public Deposit create(@RequestBody Deposit almoxarifado) {
        return almoxarifadoService.save(almoxarifado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Deposit> update(@PathVariable Long id, @RequestBody Deposit almoxarifado) {
        if (almoxarifadoService.findById(id) != null) {
            almoxarifado.setIdDeposit(id);
            return ResponseEntity.ok(almoxarifadoService.save(almoxarifado));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (almoxarifadoService.findById(id) != null) {
            almoxarifadoService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
