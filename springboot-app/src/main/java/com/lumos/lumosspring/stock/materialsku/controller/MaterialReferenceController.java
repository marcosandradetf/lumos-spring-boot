package com.lumos.lumosspring.stock.materialsku.controller;

import java.util.Objects;

import com.lumos.lumosspring.stock.materialsku.dto.MaterialRequest;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRepository;
import com.lumos.lumosspring.stock.materialstock.repository.PagedResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialsku.service.MaterialReferenceService;

@RestController
@RequestMapping("/api/material")
public class MaterialReferenceController {
    private final MaterialReferenceService materialReferenceService;
    private final MaterialReferenceRepository materialReferenceRepository;
    private final MaterialStockRepository materialStockRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public MaterialReferenceController(MaterialReferenceService materialReferenceService, MaterialReferenceRepository materialReferenceRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, MaterialStockRepository materialStockRepository) {
        this.materialReferenceService = materialReferenceService;
        this.materialReferenceRepository = materialReferenceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.materialStockRepository = materialStockRepository;
    }

    // Endpoint para retornar todos os materiais
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    public ResponseEntity<PagedResponse<MaterialResponse>> getAllMaterials(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "depositId", required = false, defaultValue = "-1") Long depositId) {

        return materialReferenceService.findAll(page, size, depositId);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    public ResponseEntity<PagedResponse<MaterialResponse>> getMaterialByNameStartingWith(
            @RequestParam(value = "name") String name,  // 'name' vindo da URL
            @RequestParam(value = "page", defaultValue = "0") Integer page,  // 'page' com valor padrão
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "depositId", required = false, defaultValue = "-1") Long depositId) {  // 'size' com valor padrão

        return materialReferenceService.searchMaterialStock(name, page, size, depositId);  // Retorna os materiais no formato de resposta
    }


    @GetMapping("/{id}")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        var name = materialStockRepository.GetNameById(id);
        return ResponseEntity.ok(Objects.requireNonNullElse(name, ""));
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

//    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
//    @PutMapping("{materialId}")
//    public ResponseEntity<?> update(@RequestBody MaterialRequest material, @PathVariable Long materialId, @CookieValue("refreshToken") String refreshToken) {
//        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
//        if (tokenFromDb.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//        var userUUID = tokenFromDb.get().getUser().getUserId();
//
//        return materialService.update(material, materialId, userUUID);
//    }
//
//    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> delete(@PathVariable Long id, @CookieValue("refreshToken") String refreshToken) {
//        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
//        if (tokenFromDb.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//        var userUUID = tokenFromDb.get().getUser().getUserId();
//
//        return materialService.deleteById(id, userUUID);
//    }





















    @GetMapping("/get-all")
    public ResponseEntity<?> findAllForImportPreMeasurement() {
        return materialReferenceService.findAllForImportPreMeasurement();
    }


}
