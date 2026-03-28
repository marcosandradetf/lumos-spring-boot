package com.lumos.lumosspring.plan.controller;

import com.lumos.lumosspring.plan.model.Module;
import com.lumos.lumosspring.plan.service.ModuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/module")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping
    public List<Module> getAll() {
        return moduleService.findAll();
    }

    @GetMapping("/{moduleCode}")
    public ResponseEntity<Module> getById(@PathVariable String moduleCode) {
        Module module = moduleService.findById(moduleCode);
        return module != null ? ResponseEntity.ok(module) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Module module) {
        return moduleService.save(module);
    }

    @PutMapping("/{moduleCode}")
    public ResponseEntity<?> update(@PathVariable String moduleCode, @RequestBody Module module) {
        return moduleService.update(moduleCode, module);
    }

    @DeleteMapping("/{moduleCode}")
    public ResponseEntity<?> delete(@PathVariable String moduleCode) {
        return moduleService.delete(moduleCode);
    }
}
