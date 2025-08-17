package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.dto.stock.DepositDTO;
import com.lumos.lumosspring.dto.stock.DepositResponse;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.service.DepositService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposit")
public class DepositController {
    @Autowired
    private DepositService depositService;

    @GetMapping
    public List<DepositResponse> getAll() {
        return depositService.findAll();
    }

    @GetMapping("/truck-deposits")
    public List<DepositResponse> findAllTruckDeposit() {
        return depositService.findAllTruckDeposit();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Deposit> getById(@PathVariable Long id) {
        Deposit deposit = depositService.findById(id);
        return deposit != null ? ResponseEntity.ok(deposit) : ResponseEntity.notFound().build();
    }

    @PostMapping("/insert")
    public ResponseEntity<?> create(@RequestBody DepositDTO deposit) {
        return depositService.save(deposit);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DepositDTO deposit) {
        return depositService.update(id, deposit);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return depositService.delete(id);
    }

    @GetMapping("/get-stockists")
    public ResponseEntity<?> getStockists() {
        return depositService.getStockists();
    }

    @GetMapping("/get-deposits-by-stockist")
    public ResponseEntity<?> getDepositStockists(
            @RequestParam String userId
    ) {
        return depositService.getDepositStockist(userId);
    }
}
