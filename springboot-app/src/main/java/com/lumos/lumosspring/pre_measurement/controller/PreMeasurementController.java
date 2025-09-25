package com.lumos.lumosspring.pre_measurement.controller;

import com.lumos.lumosspring.dto.pre_measurement.PreMeasurementDTO;
import com.lumos.lumosspring.pre_measurement.service.PreMeasurementService;
import com.lumos.lumosspring.util.ExecutionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api")
public class PreMeasurementController {
    private final PreMeasurementService preMeasurementService;

    public PreMeasurementController(PreMeasurementService preMeasurementService) {
        this.preMeasurementService = preMeasurementService;
    }

    @PostMapping("/mobile/execution/insert-pre-measurement")
    public ResponseEntity<?> saveMeasurement(
            @RequestBody PreMeasurementDTO measurementDTO
    ) {
        return preMeasurementService.savePreMeasurement(measurementDTO);
    }

    @PostMapping("/mobile/pre-measurement-street/upload-photos")
    public ResponseEntity<?> savePhotoPreMeasurement(
            @RequestPart("photos") List<MultipartFile> photos
    ) {
        return preMeasurementService.saveStreetPhotos(photos);
    }


    @GetMapping("/execution/get-pre-measurements/pending")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getMeasurements() {
        return preMeasurementService.getAll(ExecutionStatus.PENDING);
    }

    @GetMapping("/execution/get-pre-measurements/waiting")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getWaitingMeasurements() {
        return preMeasurementService.getAll(ExecutionStatus.PENDING);
    }

    @GetMapping("/execution/check-balance-pre-measurement/{preMeasurementId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> checkBalance(@PathVariable Long preMeasurementId) {
        return preMeasurementService.checkBalance(preMeasurementId);
    }


    @GetMapping("/execution/get-pre-measurement/{preMeasurementId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getPreMeasurementNotAssigned(@PathVariable long preMeasurementId) {
        return preMeasurementService.findById(preMeasurementId);
    }

    @GetMapping("/execution/get-pre-measurements/available")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getValidatedMeasurements() {
        return preMeasurementService.getAll(ExecutionStatus.AVAILABLE);
    }


    @PostMapping("/pre-measurement/mark-as-available/{id}")
    public ResponseEntity<?> markAsAvailable(@PathVariable Long id) {
        return preMeasurementService.markAsAvailable(id);
    }

//    @PostMapping("/pre-measurement/send-modifications")
//    public ResponseEntity<?> saveModifications(@RequestBody ModificationsDTO modificationsDTO) {
//        return this.preMeasurementService.saveModifications(modificationsDTO);
//    }

//    @PostMapping("/pre-measurement/import")
//    public ResponseEntity<?> importPreMeasurements(@RequestBody PreMeasurementDTO preMeasurementDTO, @RequestHeader("UUID") String userUUID) {
//        return this.preMeasurementService.importPreMeasurements(preMeasurementDTO, userUUID);
//    }

}
