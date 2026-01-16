package com.lumos.lumosspring.stock.materialstock.controller;

import com.lumos.lumosspring.stock.materialstock.dto.StockMovementDTO;
import com.lumos.lumosspring.stock.materialstock.repository.StockQueryRepository;
import com.lumos.lumosspring.stock.materialstock.service.StockMovementService;
import com.lumos.lumosspring.stock.materialstock.service.MaterialStockRegisterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MaterialStockRegisterController {
    private final StockMovementService stockMovementService;
    private final MaterialStockRegisterService materialStockRegisterService;

    public MaterialStockRegisterController(
            StockMovementService stockMovementService,
            MaterialStockRegisterService materialStockRegisterService
    ) {
        this.stockMovementService = stockMovementService;
        this.materialStockRegisterService = materialStockRegisterService;
    }

    @PostMapping("/mobile/stock/send-order")
    public ResponseEntity<StockQueryRepository.StockResponse> sendOrder(
            @RequestParam(value = "uuid") String uuid,
            @RequestBody StockQueryRepository.OrderWithItems order
    ) {
        return materialStockRegisterService.saveOrder(uuid, order);
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'ESTOQUISTA_CHEFE', 'ESTOQUISTA')")
    @PostMapping("/stock/stock-movement/create")
    public ResponseEntity<?> stockMovement(@RequestBody List<StockMovementDTO> movement) {
        return stockMovementService.createMovement(movement);
    }

    @GetMapping("/stock/stock-movement/get")
    public ResponseEntity<?> stockMovementGet() {
        return stockMovementService.stockMovementGet();
    }

    @GetMapping("/stock/stock-movement/get-approved")
    public ResponseEntity<?> stockMovementGetApproved() {
        return stockMovementService.stockMovementGetApproved();
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('ESTOQUISTA_CHEFE')")
    @PostMapping("/stock/stock-movement/approve/{movementId}")
    public ResponseEntity<?> stockMovementApprove(@PathVariable Long movementId, @CookieValue("refreshToken") String refreshToken) {
        return stockMovementService.approveStockMovement(movementId, refreshToken);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('ESTOQUISTA_CHEFE')")
    @PostMapping("/stock/stock-movement/reject/{movementId}")
    public ResponseEntity<?> stockMovementReject(@PathVariable Long movementId, @CookieValue("refreshToken") String refreshToken) {
        return stockMovementService.rejectStockMovement(movementId, refreshToken);
    }

//    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'ESTOQUISTA_CHEFE', 'ESTOQUISTA')")
//    @PostMapping("/stock/create-supplier")
//    public ResponseEntity<?> createSupplier(@RequestBody List<SupplierDTO> supplierDTO) {
//
//        for (SupplierDTO supplier : supplierDTO) {
//            // Verifica se o nome do fornecedor já existe
//            if (supplierRepository.findBySupplierName(supplier.name()) != null) {
//                return ResponseEntity.status(HttpStatus.CONFLICT).body("Foi informado um fornecedor já existente.");
//            }
//
//            var newSupplier = new Supplier();
//
//            // Setando os dados, mas ignorando os campos vazios
//            newSupplier.setSupplierAddress(util.isEmpty(supplier.address()) ? null : supplier.address());
//            newSupplier.setSupplierName(util.isEmpty(supplier.name()) ? null : supplier.name());
//            newSupplier.setSupplierEmail(util.isEmpty(supplier.email()) ? null : supplier.email());
//            newSupplier.setSupplierPhone(util.isEmpty(supplier.phone()) ? null : supplier.phone());
//            newSupplier.setSupplierContact(util.isEmpty(supplier.contact()) ? null : supplier.contact());
//            newSupplier.setSupplierCnpj(util.isEmpty(supplier.cnpj()) ? null : supplier.cnpj());
//
//            // Salva o fornecedor no banco de dados
//            supplierRepository.save(newSupplier);
//        }
//
//        // Retorna todos os fornecedores após o cadastro
//        var suppliers = supplierRepository.findAll();
//        List<SupplierResponse> supplierDTOS = StreamSupport
//                .stream(suppliers.spliterator(), false)
//                .map(SupplierResponse::new)
//                .toList();
//        return ResponseEntity.ok(supplierDTOS);
//    }


//    @GetMapping("/stock/get-suppliers")
//    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
//        var suppliers = supplierRepository.findAll();
//        List<SupplierResponse> supplierDTOS = StreamSupport
//                .stream(suppliers.spliterator(), false)
//                .map(SupplierResponse::new)
//                .toList();
//
//        return ResponseEntity.ok(supplierDTOS);
//    }


}
