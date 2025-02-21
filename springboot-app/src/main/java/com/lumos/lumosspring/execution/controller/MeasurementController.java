package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/execution")
public class MeasurementController {
    private final MeasurementService measurementService;


    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/measurements")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getMeasurements() {
        return measurementService.getAll();
    }

    @GetMapping("/pre-measurement/get-cities")
    public ResponseEntity<?> getCities() {
        return measurementService.getCities();
    }
}
