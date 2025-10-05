package com.lumos.lumosspring.premeasurement.controller.premeasurement;

import com.lumos.lumosspring.premeasurement.dto.premeasurement.DelegateDTO;
import com.lumos.lumosspring.premeasurement.service.premeasurement.PreMeasurementManagementService;
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
