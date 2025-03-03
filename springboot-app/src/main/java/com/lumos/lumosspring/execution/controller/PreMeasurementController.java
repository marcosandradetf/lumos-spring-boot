package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.controller.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.service.PreMeasurementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;


@RestController
@RequestMapping("/api")
public class PreMeasurementController {
    private final PreMeasurementService preMeasurementService;

    public PreMeasurementController(PreMeasurementService preMeasurementService) {
        this.preMeasurementService = preMeasurementService;
    }


    @PostMapping("/mobile/execution/insert-measurement")
    public ResponseEntity<?> saveMeasurement(@RequestBody PreMeasurementDTO measurementDTO, @RequestHeader("UUID") String userUUID) {
        return preMeasurementService.saveMeasurement(measurementDTO, userUUID);
    }

    @GetMapping("/execution/get-pre-measurements")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ANALISTA') or hasAuthority('SCOPE_RESPONSAVEL_TECNICO') ")
    public ResponseEntity<?> getMeasurements() {
        return preMeasurementService.getAll();
    }

}
