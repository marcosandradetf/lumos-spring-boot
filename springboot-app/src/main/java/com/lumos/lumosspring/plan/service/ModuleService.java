package com.lumos.lumosspring.plan.service;

import com.lumos.lumosspring.plan.model.Module;
import com.lumos.lumosspring.plan.repository.ModuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;

    public ModuleService(ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    public List<Module> findAll() {
        return moduleRepository.findAllByOrderByModuleCodeAsc();
    }

    public Module findById(String moduleCode) {
        return moduleRepository.findById(moduleCode).orElse(null);
    }

    public ResponseEntity<?> save(Module module) {
        if (module.getModuleCode() == null || module.getModuleCode().isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "moduleCode é obrigatório."));
        }
        if (moduleRepository.existsById(module.getModuleCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Módulo já existe."));
        }
        if (module.getIsActive() == null) {
            module.setIsActive(true);
        }
        if (module.getCreatedAt() == null) {
            module.setCreatedAt(OffsetDateTime.now());
        }
        moduleRepository.save(module);
        return ResponseEntity.ok(findAll());
    }

    public ResponseEntity<?> update(String moduleCode, Module module) {
        var existing = moduleRepository.findById(moduleCode).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setName(module.getName());
        existing.setDescription(module.getDescription());
        if (module.getIsActive() != null) {
            existing.setIsActive(module.getIsActive());
        }
        moduleRepository.save(existing);
        return ResponseEntity.ok(findAll());
    }

    public ResponseEntity<?> delete(String moduleCode) {
        if (!moduleRepository.existsById(moduleCode)) {
            return ResponseEntity.notFound().build();
        }
        moduleRepository.deleteById(moduleCode);
        return ResponseEntity.ok(findAll());
    }
}
