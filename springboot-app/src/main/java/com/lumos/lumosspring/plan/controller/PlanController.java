package com.lumos.lumosspring.plan.controller;

import com.lumos.lumosspring.plan.model.Plan;
import com.lumos.lumosspring.plan.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<Plan> getAll() {
        return planService.findAll();
    }

    @GetMapping("/{planName}")
    public ResponseEntity<Plan> getById(@PathVariable String planName) {
        Plan plan = planService.findById(planName);
        return plan != null ? ResponseEntity.ok(plan) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Plan plan) {
        return planService.save(plan);
    }

    @PutMapping("/{planName}")
    public ResponseEntity<?> update(@PathVariable String planName, @RequestBody Plan plan) {
        return planService.update(planName, plan);
    }

    @DeleteMapping("/{planName}")
    public ResponseEntity<?> delete(@PathVariable String planName) {
        return planService.delete(planName);
    }
}
