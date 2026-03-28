package com.lumos.lumosspring.plan.service;

import com.lumos.lumosspring.plan.model.PlanModule;
import com.lumos.lumosspring.plan.model.PlanModuleId;
import com.lumos.lumosspring.plan.repository.ModuleRepository;
import com.lumos.lumosspring.plan.repository.PlanModuleRepository;
import com.lumos.lumosspring.plan.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PlanModuleService {

    private final PlanModuleRepository planModuleRepository;
    private final PlanRepository planRepository;
    private final ModuleRepository moduleRepository;

    public PlanModuleService(
            PlanModuleRepository planModuleRepository,
            PlanRepository planRepository,
            ModuleRepository moduleRepository) {
        this.planModuleRepository = planModuleRepository;
        this.planRepository = planRepository;
        this.moduleRepository = moduleRepository;
    }

    public List<PlanModule> findByPlanName(String planName) {
        return planModuleRepository.findByPlanName(planName);
    }

    public List<PlanModule> findByModuleCode(String moduleCode) {
        return planModuleRepository.findByModuleCode(moduleCode);
    }

    public PlanModule findById(PlanModuleId id) {
        return planModuleRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(PlanModule planModule) {
        if (planModule.getPlanName() == null
                || planModule.getPlanName().isBlank()
                || planModule.getModuleCode() == null
                || planModule.getModuleCode().isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "planName e moduleCode são obrigatórios."));
        }
        if (!planRepository.existsById(planModule.getPlanName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Plano não encontrado."));
        }
        if (!moduleRepository.existsById(planModule.getModuleCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Módulo não encontrado."));
        }
        PlanModuleId id = planModule.toId();
        if (planModuleRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Associação plano/módulo já existe."));
        }
        if (planModule.getEnabled() == null) {
            planModule.setEnabled(true);
        }
        planModuleRepository.insert(planModule);
        return ResponseEntity.ok(findByPlanName(planModule.getPlanName()));
    }

    public ResponseEntity<?> update(PlanModuleId id, PlanModule planModule) {
        var existing = planModuleRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (planModule.getEnabled() != null) {
            existing.setEnabled(planModule.getEnabled());
        }
        planModuleRepository.update(existing);
        return ResponseEntity.ok(findByPlanName(id.getPlanName()));
    }

    public ResponseEntity<?> delete(PlanModuleId id) {
        if (!planModuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        planModuleRepository.deleteById(id);
        return ResponseEntity.ok(findByPlanName(id.getPlanName()));
    }
}
