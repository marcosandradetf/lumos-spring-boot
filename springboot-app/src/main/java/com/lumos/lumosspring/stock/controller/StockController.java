package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.stock.controller.dto.*;
import com.lumos.lumosspring.stock.entities.Supplier;
import com.lumos.lumosspring.stock.repository.SupplierRepository;
import com.lumos.lumosspring.stock.service.StockMovementService;
import com.lumos.lumosspring.util.Util;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final StockMovementService stockMovementService;
    private final SupplierRepository supplierRepository;
    private final Util util;

    public StockController(StockMovementService stockMovementService, SupplierRepository supplierRepository, Util util) {
        this.stockMovementService = stockMovementService;
        this.supplierRepository = supplierRepository;
        this.util = util;
    }


    @GetMapping("/get-suppliers")
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();
        List<SupplierResponse> supplierDTOS = suppliers.stream().map(SupplierResponse::new).toList(); // Converte diretamente para Page<MaterialResponse>
        return ResponseEntity.ok(supplierDTOS);
    }

    @PostMapping("/stock-movement/create")
    public ResponseEntity<?> stockMovement(@RequestBody List<StockMovementDTO> movement, @CookieValue("refreshToken") String refreshToken) {
        return stockMovementService.createMovement(movement, refreshToken);
    }

    @GetMapping("/stock-movement/get")
    public ResponseEntity<?> stockMovementGet() {
        return stockMovementService.stockMovementGet();
    }

    @GetMapping("/stock-movement/get-approved")
    public ResponseEntity<?> stockMovementGetApproved() {
        return stockMovementService.stockMovementGetApproved();
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping("/stock-movement/approve/{movementId}")
    public ResponseEntity<?> stockMovementApprove(@PathVariable Long movementId, @CookieValue("refreshToken") String refreshToken) {
        return stockMovementService.approveStockMovement(movementId, refreshToken);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping("/stock-movement/reject/{movementId}")
    public ResponseEntity<?> stockMovementReject(@PathVariable Long movementId, @CookieValue("refreshToken") String refreshToken) {
        return stockMovementService.rejectStockMovement(movementId, refreshToken);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping("/create-supplier")
    public ResponseEntity<?> createSupplier(@RequestBody List<SupplierDTO> supplierDTO) {

        for (SupplierDTO supplier : supplierDTO) {
            // Verifica se o nome do fornecedor já existe
            if (supplierRepository.findBySupplierName(supplier.name()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Foi informado um fornecedor já existente.");
            }

            var newSupplier = new Supplier();

            // Setando os dados, mas ignorando os campos vazios
            newSupplier.setSupplierAddress(util.isEmpty(supplier.address()) ? null : supplier.address());
            newSupplier.setSupplierName(util.isEmpty(supplier.name()) ? null : supplier.name());
            newSupplier.setSupplierEmail(util.isEmpty(supplier.email()) ? null : supplier.email());
            newSupplier.setSupplierPhone(util.isEmpty(supplier.phone()) ? null : supplier.phone());
            newSupplier.setSupplierContact(util.isEmpty(supplier.contact()) ? null : supplier.contact());
            newSupplier.setSupplierCnpj(util.isEmpty(supplier.cnpj()) ? null : supplier.cnpj());

            // Salva o fornecedor no banco de dados
            supplierRepository.save(newSupplier);
        }

        // Retorna todos os fornecedores após o cadastro
        List<Supplier> suppliers = supplierRepository.findAll();
        List<SupplierResponse> supplierDTOS = suppliers.stream().map(SupplierResponse::new).toList();
        return ResponseEntity.ok(supplierDTOS);
    }
}
