package com.lumos.lumosspring.stock.materialsku.controller;

import com.lumos.lumosspring.stock.materialsku.dto.MaterialRequest;
import com.lumos.lumosspring.stock.materialsku.model.Material;
import com.lumos.lumosspring.stock.materialsku.service.MaterialReferenceService;
import com.lumos.lumosspring.stock.materialsku.service.MaterialReferenceViewService;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material")
public class MaterialReferenceViewController {
    private final MaterialReferenceViewService service;

    public MaterialReferenceViewController(MaterialReferenceViewService service) {
        this.service = service;
    }




}
