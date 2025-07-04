package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.stock.controller.dto.TypeDTO;
import com.lumos.lumosspring.stock.entities.MaterialType;
import com.lumos.lumosspring.stock.service.TypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/type")
public class TypeController {
    private final TypeService typeService;

    public TypeController(TypeService typeService) {
        this.typeService = typeService;
    }

    @GetMapping
    public List<MaterialType> getAll() {
        return typeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        MaterialType materialType = typeService.findById(id);
        return materialType != null ? ResponseEntity.ok(materialType) : ResponseEntity.notFound().build();
    }

    @PostMapping("/insert")
    public ResponseEntity<?> create(@RequestBody TypeDTO type) {
        return typeService.save(type);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TypeDTO typeDTO) {
        return typeService.update(id, typeDTO);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable Long id) {
       return typeService.delete(id);
    }
}
