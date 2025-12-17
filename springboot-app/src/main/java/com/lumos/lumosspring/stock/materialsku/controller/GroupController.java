package com.lumos.lumosspring.stock.materialsku.controller;

import com.lumos.lumosspring.stock.materialsku.model.MaterialGroup;
import com.lumos.lumosspring.stock.materialsku.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group")
public class GroupController {
    private final GroupService grupoService;

    public GroupController(GroupService grupoService) {
        this.grupoService = grupoService;
    }

    @GetMapping
    public List<MaterialGroup> getAll() {
        return grupoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialGroup> getById(@PathVariable Long id) {
        MaterialGroup materialGroup = grupoService.findById(id);
        return materialGroup != null ? ResponseEntity.ok(materialGroup) : ResponseEntity.notFound().build();
    }

    @PostMapping("/insert")
    public ResponseEntity<?> create(@RequestBody String groupName) {
        return grupoService.save(groupName);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody String groupName) {
        return grupoService.update(id, groupName);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return grupoService.delete(id);
    }
}
