package com.lumos.lumosspring.stock.controller;

import java.util.Objects;

import com.lumos.lumosspring.stock.repository.MaterialStockRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.stock.controller.dto.MaterialRequest;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.stock.service.MaterialService;

@RestController
@RequestMapping("/api/material")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialRepository materialRepository;
    private final MaterialStockRepository materialStockRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public MaterialController(MaterialService materialService, MaterialRepository materialRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder, MaterialStockRepository materialStockRepository) {
        this.materialService = materialService;
        this.materialRepository = materialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.materialStockRepository = materialStockRepository;
    }

    // Endpoint para retornar todos os materiais
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    public ResponseEntity<Page<MaterialResponse>> getAllMaterials(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "depositId", required = false, defaultValue = "-1") Long depositId) {

        return materialService.findAll(page, size, depositId);
    }


//    @GetMapping("{pIdMaterial}")
//    public ResponseEntity<MaterialResponse> getMaterial(@PathVariable Long pIdMaterial) {
//        Material material = materialService.findById(pIdMaterial);
//        MaterialResponse materialsDTO = material.map(MaterialResponse::new); // Converte diretamente para Page<MaterialResponse>
//        return ResponseEntity.ok(materialsDTO);
//    }


    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    public ResponseEntity<Page<MaterialResponse>> getMaterialByNameStartingWith(
            @RequestParam(value = "name") String name,  // 'name' vindo da URL
            @RequestParam(value = "page", defaultValue = "0") int page,  // 'page' com valor padrão
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "depositId", required = false, defaultValue = "-1") Long depositId) {  // 'size' com valor padrão

        return materialService.searchMaterialStock(name, page, size, depositId);  // Retorna os materiais no formato de resposta
    }


    @GetMapping("/{id}")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        var name = materialStockRepository.GetNameById(id);
        return ResponseEntity.ok(Objects.requireNonNullElse(name, ""));
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    @PostMapping
    //public ResponseEntity<String>  create(@RequestBody Material material, @CookieValue("refreshToken") String refreshToken) {
    public ResponseEntity<?> create(@RequestBody MaterialRequest material) {
        //return materialService.save(material, UUID.fromString(refreshToken));
        return materialService.save(material);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    @PutMapping("{materialId}")
    public ResponseEntity<?> update(@RequestBody MaterialRequest material, @PathVariable Long materialId, @CookieValue("refreshToken") String refreshToken) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var userUUID = tokenFromDb.get().getUser().getUserId();

        return materialService.update(material, materialId, userUUID);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_ESTOQUISTA_CHEFE') or hasAuthority('SCOPE_ESTOQUISTA')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @CookieValue("refreshToken") String refreshToken) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var userUUID = tokenFromDb.get().getUser().getUserId();

        return materialService.deleteById(id, userUUID);
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> findAllForImportPreMeasurement() {
        return materialService.findAllForImportPreMeasurement();
    }
}
