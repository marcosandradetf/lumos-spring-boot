package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.stock.controller.dto.MaterialRequest;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.controller.dto.StockMovementDTO;
import com.lumos.lumosspring.stock.controller.dto.SupplierDTO;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.entities.Supplier;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.stock.repository.SupplierRepository;
import com.lumos.lumosspring.stock.service.MaterialService;
import com.lumos.lumosspring.stock.service.StockMovementService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final StockMovementService stockMovementService;
    private final SupplierRepository supplierRepository;

    public StockController(StockMovementService stockMovementService, SupplierRepository supplierRepository) {
        this.stockMovementService = stockMovementService;
        this.supplierRepository = supplierRepository;
    }


    @GetMapping("/get-suppliers")
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping("/stock-movement")
    public ResponseEntity<?> stockMovement(@RequestBody StockMovementDTO movement, @CookieValue("refreshToken") String refreshToken) {
        return stockMovementService.updateStock(movement, refreshToken);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping("/create-supplier")
    public Supplier createSupplier(@RequestBody SupplierDTO supplierDTO) {
        Supplier newSupplier = new Supplier();
        newSupplier.setSupplierAddress(supplierDTO.address());
        newSupplier.setSupplierName(supplierDTO.name());
        newSupplier.setSupplierEmail(supplierDTO.email());
        newSupplier.setSupplierPhone(supplierDTO.phone());
        newSupplier.setSupplierContact(supplierDTO.contact());
        supplierRepository.save(newSupplier);

        return newSupplier;
    }
}
