package com.lumos.lumosspring.estoque.controller;

import com.lumos.lumosspring.estoque.model.Material;
import com.lumos.lumosspring.estoque.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/material")
public class MaterialController {
    @Autowired
    private MaterialService materialService;

    @GetMapping
    public List<Material> getAll() {
        return materialService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Material> getById(@PathVariable Long id) {
        Material material = materialService.findById(id);
        return material != null ? ResponseEntity.ok(material) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Material create(@RequestBody Material material) {
        return materialService.save(material);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Material> update(@PathVariable Long id, @RequestBody Material material) {
        if (materialService.findById(id) != null) {
            material.setIdMaterial(id);
            return ResponseEntity.ok(materialService.save(material));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (materialService.findById(id) != null) {
            materialService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
