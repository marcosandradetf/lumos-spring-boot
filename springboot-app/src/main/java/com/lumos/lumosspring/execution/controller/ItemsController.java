package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/execution")
public class ItemsController {
    private final ItemService itemService;

    public ItemsController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/itens/{depositId}")
    public ResponseEntity<?> deposit(@PathVariable("depositId") Long depositId) {
        return itemService.getItems(depositId);
    }


    @PostMapping("/insert-measurement")
    public ResponseEntity<?> saveMeasurement(@RequestBody MeasurementDTO measurementDTO) {
        return itemService.saveMeasurement(measurementDTO);
    }
}
