package com.lumos.lumosspring.estoque.controller;

import com.lumos.lumosspring.estoque.model.Almoxarifado;
import com.lumos.lumosspring.estoque.service.AlmoxarifadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almoxarifado")
public class AlmoxarifadoController {
    @Autowired
    private AlmoxarifadoService almoxarifadoService;

    @GetMapping
    public List<Almoxarifado> getAll() {
        return almoxarifadoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Almoxarifado> getById(@PathVariable Long id) {
        Almoxarifado almoxarifado = almoxarifadoService.findById(id);
        return almoxarifado != null ? ResponseEntity.ok(almoxarifado) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Almoxarifado create(@RequestBody Almoxarifado almoxarifado) {
        return almoxarifadoService.save(almoxarifado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Almoxarifado> update(@PathVariable Long id, @RequestBody Almoxarifado almoxarifado) {
        if (almoxarifadoService.findById(id) != null) {
            almoxarifado.setIdAlmoxarifado(id);
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
