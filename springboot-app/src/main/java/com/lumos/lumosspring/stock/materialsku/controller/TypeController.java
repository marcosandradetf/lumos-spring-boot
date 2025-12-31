package com.lumos.lumosspring.stock.materialsku.controller;

import com.lumos.lumosspring.stock.materialsku.dto.TypeDTO;
import com.lumos.lumosspring.stock.materialsku.model.MaterialType;
import com.lumos.lumosspring.stock.materialsku.service.TypeService;
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

    @GetMapping("/get-all-type-subtype")
    public ResponseEntity<?> findAllTypeSubtype() {
        return typeService.findAllTypeSubtype();
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
