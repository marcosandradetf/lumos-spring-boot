package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.service.PreMeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/execution")
public class PreMeasurementController {
    private final PreMeasurementService preMeasurementService;

    public PreMeasurementController(PreMeasurementService preMeasurementService) {
        this.preMeasurementService = preMeasurementService;
    }

    @GetMapping("/itens/{depositId}")
    public ResponseEntity<?> deposit(@PathVariable("depositId") Long depositId) {
        return preMeasurementService.getItems(depositId);
    }


    @PostMapping("/insert-measurement")
    public ResponseEntity<?> saveMeasurement(@RequestBody MeasurementDTO measurementDTO, @RequestHeader("UUID") String userUUID) {
        return preMeasurementService.saveMeasurement(measurementDTO, userUUID);
    }
}
