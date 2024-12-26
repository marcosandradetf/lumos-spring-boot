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

    @PostMapping("/insert")
    public ResponseEntity<?> create(@RequestBody String groupName) {
        return grupoService.save(groupName);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody String groupName) {
        return ResponseEntity.ok(grupoService.update(id, groupName));
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return grupoService.delete(id);
    }
}
