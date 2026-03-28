package com.lumos.lumosspring.plan.controller;

import com.lumos.lumosspring.plan.model.PlanModule;
import com.lumos.lumosspring.plan.model.PlanModuleId;
import com.lumos.lumosspring.plan.service.PlanModuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plan-module")
public class PlanModuleController {

    private final PlanModuleService planModuleService;

    public PlanModuleController(PlanModuleService planModuleService) {
        this.planModuleService = planModuleService;
    }

    @GetMapping("/by-plan/{planName}")
    public List<PlanModule> listByPlan(@PathVariable String planName) {
        return planModuleService.findByPlanName(planName);
    }

    @GetMapping("/by-module/{moduleCode}")
    public List<PlanModule> listByModule(@PathVariable String moduleCode) {
        return planModuleService.findByModuleCode(moduleCode);
    }

    @GetMapping("/{planName}/{moduleCode}")
    public ResponseEntity<PlanModule> getById(
            @PathVariable String planName,
            @PathVariable String moduleCode) {
        PlanModuleId id = new PlanModuleId(planName, moduleCode);
        PlanModule row = planModuleService.findById(id);
        return row != null ? ResponseEntity.ok(row) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PlanModule planModule) {
        return planModuleService.save(planModule);
    }

    @PutMapping("/{planName}/{moduleCode}")
    public ResponseEntity<?> update(
            @PathVariable String planName,
            @PathVariable String moduleCode,
            @RequestBody PlanModule planModule) {
        PlanModuleId id = new PlanModuleId(planName, moduleCode);
        return planModuleService.update(id, planModule);
    }

    @DeleteMapping("/{planName}/{moduleCode}")
    public ResponseEntity<?> delete(@PathVariable String planName, @PathVariable String moduleCode) {
        PlanModuleId id = new PlanModuleId(planName, moduleCode);
        return planModuleService.delete(id);
    }
}
