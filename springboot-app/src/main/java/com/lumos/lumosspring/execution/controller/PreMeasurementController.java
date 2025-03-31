package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.service.PreMeasurementService;
import com.lumos.lumosspring.util.ContractStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class PreMeasurementController {
    private final PreMeasurementService preMeasurementService;

    public PreMeasurementController(PreMeasurementService preMeasurementService) {
        this.preMeasurementService = preMeasurementService;
    }


    @PostMapping("/mobile/execution/insert-pre-measurement")
    public ResponseEntity<?> saveMeasurement(@RequestBody PreMeasurementDTO measurementDTO, @RequestHeader("UUID") String userUUID) {
        return preMeasurementService.saveMeasurement(measurementDTO, userUUID);
    }

    @GetMapping("/execution/get-pre-measurements/pending")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getMeasurements() {
        return preMeasurementService.getAll(ContractStatus.PENDING);
    }

    @GetMapping("/execution/get-pre-measurements/{preMeasurementId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getPreMeasurement(@PathVariable long preMeasurementId) {
        return preMeasurementService.getPreMeasurement(preMeasurementId);
    }

    @GetMapping("/execution/get-pre-measurements/validating")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getValidatingMeasurements() {
        return preMeasurementService.getAll(ContractStatus.VALIDATING);
    }

    @GetMapping("/execution/get-pre-measurements/waiting")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getWaitingMeasurements() {
        return preMeasurementService.getAll(ContractStatus.WAITING);
    }

    @GetMapping("/execution/get-pre-measurements/validated")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getValidatedMeasurements() {
        return preMeasurementService.getAll(ContractStatus.VALIDATED);
    }

    @GetMapping("/execution/get-pre-measurements/in-progress")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getInProgressMeasurements() {
        return preMeasurementService.getAll(ContractStatus.IN_PROGRESS);
    }

}
