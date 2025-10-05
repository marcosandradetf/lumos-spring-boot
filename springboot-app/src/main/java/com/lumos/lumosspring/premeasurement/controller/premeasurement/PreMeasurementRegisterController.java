package com.lumos.lumosspring.premeasurement.controller.premeasurement;

import com.lumos.lumosspring.premeasurement.dto.premeasurement.PreMeasurementRequest;
import com.lumos.lumosspring.premeasurement.service.premeasurement.PreMeasurementRegisterService;
import com.lumos.lumosspring.util.ExecutionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api")
public class PreMeasurementRegisterController {
    private final PreMeasurementRegisterService registerService;

    public PreMeasurementRegisterController(PreMeasurementRegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/mobile/execution/insert-pre-measurement")
    public ResponseEntity<?> saveMeasurement(
            @RequestBody PreMeasurementRequest measurementDTO
    ) {
        return registerService.savePreMeasurement(measurementDTO);
    }

    @PostMapping("/mobile/pre-measurement-street/upload-photos")
    public ResponseEntity<?> savePhotoPreMeasurement(
            @RequestPart("photos") List<MultipartFile> photos
    ) {
        return registerService.saveStreetPhotos(photos);
    }

    @PostMapping("/pre-measurement/import")
    public ResponseEntity<?> importPreMeasurements(@RequestBody PreMeasurementRequest preMeasurementReq) {
        return registerService.importPreMeasurements(preMeasurementReq);
    }

    @GetMapping("/execution/get-pre-measurements/pending")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getMeasurements() {
        return registerService.getAll(ExecutionStatus.PENDING);
    }

    @GetMapping("/execution/get-pre-measurements/waiting")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getWaitingMeasurements() {
        return registerService.getAll(ExecutionStatus.PENDING);
    }

    @GetMapping("/execution/check-balance-pre-measurement/{preMeasurementId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> checkBalance(@PathVariable Long preMeasurementId) {
        return registerService.checkBalance(preMeasurementId);
    }

    @GetMapping("/execution/get-pre-measurement/{preMeasurementId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getPreMeasurementNotAssigned(@PathVariable long preMeasurementId) {
        return registerService.findById(preMeasurementId);
    }

    @GetMapping("/execution/get-pre-measurements/available")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getValidatedMeasurements() {
        return registerService.getAll(ExecutionStatus.AVAILABLE);
    }


    @PostMapping("/pre-measurement/mark-as-available/{id}")
    public ResponseEntity<?> markAsAvailable(@PathVariable Long id) {
        return registerService.markAsAvailable(id);
    }

//    @PostMapping("/pre-measurement/send-modifications")
//    public ResponseEntity<?> saveModifications(@RequestBody ModificationsDTO modificationsDTO) {
//        return this.preMeasurementService.saveModifications(modificationsDTO);
//    }



}
