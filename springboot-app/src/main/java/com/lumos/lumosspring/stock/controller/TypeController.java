package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.stock.service.TypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/type")
public class TypeController {
    @Autowired
    private TypeService typeService;

    @GetMapping
    public List<Type> getAll() {
        return typeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Type> getById(@PathVariable Long id) {
        Type type = typeService.findById(id);
        return type != null ? ResponseEntity.ok(type) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Type create(@RequestBody Type type) {
        return typeService.save(type);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Type> update(@PathVariable Long id, @RequestBody Type type) {
        if (typeService.findById(id) != null) {
            type.setIdType(id);
            return ResponseEntity.ok(typeService.save(type));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (typeService.findById(id) != null) {
            typeService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
