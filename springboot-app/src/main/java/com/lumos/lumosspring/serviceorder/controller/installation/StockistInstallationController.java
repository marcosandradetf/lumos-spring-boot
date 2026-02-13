package com.lumos.lumosspring.serviceorder.controller.installation;


import com.lumos.lumosspring.serviceorder.dto.installation.ReserveDTOCreate;
import com.lumos.lumosspring.serviceorder.service.installation.StockistInstallationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class StockistInstallationController {
    private final StockistInstallationService stockistInstallationService;

    public StockistInstallationController(StockistInstallationService stockistInstallationService) {

        this.stockistInstallationService = stockistInstallationService;
    }

    @GetMapping("/execution/get-reservations/{userUUID}")
    public ResponseEntity<?> getPendingReservesForStockist(@PathVariable UUID userUUID) {
        return stockistInstallationService.getPendingReservesForStockist(userUUID);
    }

    @PostMapping("/execution/reserve-materials-for-execution/{userUUID}")
    public ResponseEntity<?> reserveMaterialsForExecution(
            @RequestBody ReserveDTOCreate reserveDTOCreate,
            @PathVariable String userUUID
    ) {
        return stockistInstallationService.reserveMaterialsForExecution(reserveDTOCreate, userUUID);
    }

    @GetMapping("/execution/get-executions/{status}")
    public ResponseEntity<?> getExecutions(
            @PathVariable String status,
            @RequestParam(required = false) Long contractId
    ) {
        return stockistInstallationService.getExecutions(status, contractId);
    }


    @PostMapping("/execution/update-managements")
    public ResponseEntity<?> updateManagements(
            @RequestBody PayloadManagement payload,
            @RequestParam String status
    ) {
        return stockistInstallationService.updateManagements(payload, status);
    }
    public record PayloadManagement(
            List<Long> deleted,
            List<PayloadUpdate> updated
    ) {}
    public record PayloadUpdate(
            Long reservationManagementId,
            UUID userId,
            Long teamId
    ) {}


}
