package com.lumos.lumosspring.estoque.controller;

import com.lumos.lumosspring.estoque.model.Grupo;
import com.lumos.lumosspring.estoque.service.GrupoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/grupo")
public class GrupoController {
    @Autowired
    private GrupoService grupoService;

    @GetMapping
    public List<Grupo> getAll() {
        return grupoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grupo> getById(@PathVariable Long id) {
        Grupo grupo = grupoService.findById(id);
        return grupo != null ? ResponseEntity.ok(grupo) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Grupo create(@RequestBody Grupo grupo) {
        return grupoService.save(grupo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grupo> update(@PathVariable Long id, @RequestBody Grupo grupo) {
        if (grupoService.findById(id) != null) {
            grupo.setIdGrupo(id);
            return ResponseEntity.ok(grupoService.save(grupo));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (grupoService.findById(id) != null) {
            grupoService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
