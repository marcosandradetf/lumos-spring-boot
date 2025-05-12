package com.lumos.lumosspring.pre_measurement.controller;

import com.lumos.lumosspring.pre_measurement.dto.ModificationsDTO;
import com.lumos.lumosspring.pre_measurement.dto.PreMeasurementDTO;
import com.lumos.lumosspring.pre_measurement.service.PreMeasurementService;
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
        return preMeasurementService.savePreMeasurement(measurementDTO, userUUID);
    }

    @GetMapping("/execution/get-pre-measurement/{preMeasurementId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getPreMeasurement(@PathVariable long preMeasurementId) {
        return preMeasurementService.getPreMeasurement(preMeasurementId);
    }

    @GetMapping("/execution/get-pre-measurements/pending")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getMeasurements() {
        return preMeasurementService.getAll(ContractStatus.PENDING);
    }

    @GetMapping("/execution/get-pre-measurements/waiting")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getWaitingMeasurements() {
        return preMeasurementService.getAll(ContractStatus.WAITING_CONTRACTOR);
    }

    @GetMapping("/execution/get-pre-measurements/validating")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getValidatingMeasurements() {
        return preMeasurementService.getAll(ContractStatus.VALIDATING);
    }

    @GetMapping("/execution/get-pre-measurements/available")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getValidatedMeasurements() {
        return preMeasurementService.getAll(ContractStatus.AVAILABLE);
    }

    @GetMapping("/execution/get-pre-measurements/in-progress")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getInProgressMeasurements() {
        return preMeasurementService.getAll(ContractStatus.IN_PROGRESS);
    }

    @PostMapping("/pre-measurement/evolve-status/{id}")
    public ResponseEntity<?> evolveStatus(@PathVariable Long id) {
        var state =  preMeasurementService.setStatus(id);
        if(state) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/pre-measurement/send-modifications")
    public ResponseEntity<?> saveModifications(@RequestBody ModificationsDTO modificationsDTO) {
        return this.preMeasurementService.saveModifications(modificationsDTO);
    }

    @PostMapping("/pre-measurement/import")
    public ResponseEntity<?> importPreMeasurements(@RequestBody PreMeasurementDTO preMeasurementDTO, @RequestHeader("UUID") String userUUID) {
        return this.preMeasurementService.importPreMeasurements(preMeasurementDTO, userUUID);
    }

}
