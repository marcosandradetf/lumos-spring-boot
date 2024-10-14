package com.lumos.lumosspring.estoque.controller;

import com.lumos.lumosspring.estoque.model.Tipo;
import com.lumos.lumosspring.estoque.service.TipoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tipo")
public class TipoController {
    @Autowired
    private TipoService tipoService;

    @GetMapping
    public List<Tipo> getAll() {
        return tipoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tipo> getById(@PathVariable Long id) {
        Tipo tipo = tipoService.findById(id);
        return tipo != null ? ResponseEntity.ok(tipo) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Tipo create(@RequestBody Tipo tipo) {
        return tipoService.save(tipo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tipo> update(@PathVariable Long id, @RequestBody Tipo tipo) {
        if (tipoService.findById(id) != null) {
            tipo.setIdTipo(id);
            return ResponseEntity.ok(tipoService.save(tipo));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (tipoService.findById(id) != null) {
            tipoService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
