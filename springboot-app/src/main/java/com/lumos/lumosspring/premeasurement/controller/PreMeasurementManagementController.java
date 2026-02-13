package com.lumos.lumosspring.premeasurement.controller;

import com.lumos.lumosspring.premeasurement.dto.DelegateDTO;
import com.lumos.lumosspring.premeasurement.service.PreMeasurementManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class PreMeasurementManagementController {
    private final PreMeasurementManagementService preMeasurementInstallationService;

    public PreMeasurementManagementController(PreMeasurementManagementService preMeasurementInstallationService) {
        this.preMeasurementInstallationService = preMeasurementInstallationService;
    }

    @PostMapping("/execution/delegate")
    public ResponseEntity<?> delegate(@RequestBody DelegateDTO delegateDTO) {
        return preMeasurementInstallationService.delegateToStockist(delegateDTO);
    }


}
