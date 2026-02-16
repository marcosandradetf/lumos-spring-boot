package com.lumos.lumosspring.serviceorder.controller.installation;

import com.lumos.lumosspring.serviceorder.dto.installation.ReserveDTOCreate;
import com.lumos.lumosspring.serviceorder.service.installation.ManagementRegisterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/service-order")
public class ManagementRegisterController {
    private final ManagementRegisterService managementRegisterService;

    public ManagementRegisterController(ManagementRegisterService managementRegisterService) {

        this.managementRegisterService = managementRegisterService;
    }

    @PostMapping("/reserve-materials-for-execution")
    public ResponseEntity<?> reserveMaterialsForExecution(
            @RequestBody ReserveDTOCreate reserveDTOCreate
    ) {
        return managementRegisterService.reserveMaterialsForExecution(reserveDTOCreate);
    }


    @PutMapping("/update-management")
    public ResponseEntity<?> updateManagement(
            @RequestParam Long reservationManagementId,
            @RequestParam UUID userId,
            @RequestParam Long teamId
    ) {
        return managementRegisterService.updateManagement(reservationManagementId, userId, teamId);
    }

    @DeleteMapping("/delete-management")
    public ResponseEntity<?> deleteManagement(
            @RequestParam String status,
            @RequestParam Long reservationManagementId
    ) {
        return managementRegisterService.deleteManagement(status, reservationManagementId);
    }


}
