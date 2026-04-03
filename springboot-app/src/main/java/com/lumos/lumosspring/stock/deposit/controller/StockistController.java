package com.lumos.lumosspring.stock.deposit.controller;

import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.lumos.lumosspring.stock.deposit.service.StockistService;
import com.lumos.lumosspring.team.repository.StockistRepository;

@RestController
@RequestMapping("/api/stockist")
public class StockistController {
    @Autowired
    private StockistService stockistService;

    @Autowired
    private StockistRepository stockistRepository;
    
    @GetMapping
    public ResponseEntity<?> getStockists() {
        return stockistService.getStockists();
    }
    
    @PostMapping
    public ResponseEntity<?> createStockist(@RequestBody StockistRequest dto) {
        return ResponseEntity.status(201).body(stockistService.create(dto));
    }
    
    @PutMapping
    public ResponseEntity<?> updateStockist(@RequestBody StockistRequest dto) {
        return ResponseEntity.ok(stockistService.update(dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStockist(@PathVariable Long id) {
        stockistRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record StockistRequest(
        UUID userIdUser,
        Long depositIdDeposit
    ) {}
}