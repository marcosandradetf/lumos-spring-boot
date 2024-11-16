package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.stock.entities.Group;
import com.lumos.lumosspring.stock.service.GroupService;
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
    public List<Group> getAll() {
        return grupoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getById(@PathVariable Long id) {
        Group group = grupoService.findById(id);
        return group != null ? ResponseEntity.ok(group) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Group create(@RequestBody Group group) {
        return grupoService.save(group);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Group> update(@PathVariable Long id, @RequestBody Group group) {
        if (grupoService.findById(id) != null) {
            group.setIdGroup(id);
            return ResponseEntity.ok(grupoService.save(group));
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
