package com.lumos.lumosspring.stock.order.controller;


import com.lumos.lumosspring.stock.order.dto.ReserveDTOCreate;
import com.lumos.lumosspring.stock.order.service.StockistInstallationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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



}
