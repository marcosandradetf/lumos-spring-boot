package com.lumos.lumosspring.plan.service;

import com.lumos.lumosspring.plan.model.Plan;
import com.lumos.lumosspring.plan.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> findAll() {
        return planRepository.findAllByOrderByPlanNameAsc();
    }

    public Plan findById(String planName) {
        return planRepository.findById(planName).orElse(null);
    }

    public ResponseEntity<?> save(Plan plan) {
        if (plan.getPlanName() == null || plan.getPlanName().isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "planName é obrigatório."));
        }
        if (planRepository.existsById(plan.getPlanName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Plano já existe."));
        }
        if (plan.getIsActive() == null) {
            plan.setIsActive(true);
        }
        if (plan.getCreatedAt() == null) {
            plan.setCreatedAt(OffsetDateTime.now());
        }
        planRepository.save(plan);
        return ResponseEntity.ok(findAll());
    }

    public ResponseEntity<?> update(String planName, Plan plan) {
        var existing = planRepository.findById(planName).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setDescription(plan.getDescription());
        existing.setPricePerUserMonthly(plan.getPricePerUserMonthly());
        existing.setPricePerUserYearly(plan.getPricePerUserYearly());
        if (plan.getIsActive() != null) {
            existing.setIsActive(plan.getIsActive());
        }
        planRepository.save(existing);
        return ResponseEntity.ok(findAll());
    }

    public ResponseEntity<?> delete(String planName) {
        if (!planRepository.existsById(planName)) {
            return ResponseEntity.notFound().build();
        }
        planRepository.deleteById(planName);
        return ResponseEntity.ok(findAll());
    }
}
