package com.lumos.lumosspring.stock.materialsku.controller;

import java.util.Objects;

import com.lumos.lumosspring.stock.materialsku.dto.MaterialRequest;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.lumos.lumosspring.stock.materialsku.service.MaterialReferenceService;

@RestController
@RequestMapping("/api/material")
public class MaterialReferenceController {
    private final MaterialReferenceService materialReferenceService;
    private final MaterialStockRegisterRepository materialStockRegisterRepository;

    public MaterialReferenceController(MaterialReferenceService materialReferenceService,
                                       MaterialStockRegisterRepository materialStockRegisterRepository) {
        this.materialReferenceService = materialReferenceService;
        this.materialStockRegisterRepository = materialStockRegisterRepository;
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody MaterialRequest material) {
        return materialReferenceService.create(material);
    }

    @GetMapping("/find-by-barcode")
    public ResponseEntity<?> findByBarcode(
            @RequestParam("barcode") String barcode
    ) {

        return materialReferenceService.findByBarcode(barcode);
    }

    @GetMapping("/find-by-id")
    public ResponseEntity<?> findById(
            @RequestParam("materialId") Long materialId
    ) {
        return materialReferenceService.findById(materialId);
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> findAllForImportPreMeasurement() {
        return materialReferenceService.findAllForImportPreMeasurement();
    }

    @GetMapping("/get-catalogue")
    public ResponseEntity<?> getCatalogue() {
        return materialReferenceService.getCatalogue();
    }
}
