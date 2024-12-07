package com.lumos.lumosspring.stock.controller;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.stock.controller.dto.MaterialRequest;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.stock.service.MaterialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/material")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialRepository materialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtDecoder jwtDecoder;

    public MaterialController(MaterialService materialService, MaterialRepository materialRepository, RefreshTokenRepository refreshTokenRepository, JwtDecoder jwtDecoder) {
        this.materialService = materialService;
        this.materialRepository = materialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtDecoder = jwtDecoder;
    }

    // Endpoint para retornar todos os materiais
    @GetMapping
    public ResponseEntity<Page<MaterialResponse>> getAllMaterials(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<Material> materials = materialService.findAll(page, size);
        Page<MaterialResponse> materialsDTO = materials.map(MaterialResponse::new); // Converte diretamente para Page<MaterialResponse>
        return ResponseEntity.ok(materialsDTO);
    }

    @GetMapping("/filter-by-deposit")
    public ResponseEntity<Page<MaterialResponse>> getMaterialsByDeposit(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "deposit") List<Long> depositId
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Material> materials = materialRepository.findByDeposit(pageable, depositId);
        Page<MaterialResponse> materialsDTO = materials.map(MaterialResponse::new);
        return ResponseEntity.ok(materialsDTO);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MaterialResponse>> getMaterialByNameStartingWith(
            @RequestParam(value = "name") String name,  // 'name' vindo da URL
            @RequestParam(value = "page", defaultValue = "0") int page,  // 'page' com valor padrão
            @RequestParam(value = "size", defaultValue = "10") int size) {  // 'size' com valor padrão

        Pageable pageable = PageRequest.of(page, size);  // Configura o Pageable para a paginação

        // Busca materiais que começam com 'name' (parâmetro passado na URL)
        Page<Material> materials = materialRepository.findByMaterialNameOrTypeIgnoreAccent(pageable, name.toLowerCase());

        // Converte a lista de materiais para o DTO MaterialResponse
        Page<MaterialResponse> materialsDTO = materials.map(MaterialResponse::new);

        return ResponseEntity.ok(materialsDTO);  // Retorna os materiais no formato de resposta
    }


    @GetMapping("/{id}")
    public ResponseEntity<String>  getById(@PathVariable Long id) {
        var name = materialRepository.GetNameById(id);
        return ResponseEntity.ok(Objects.requireNonNullElse(name, ""));
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping
    //public ResponseEntity<String>  create(@RequestBody Material material, @CookieValue("refreshToken") String refreshToken) {
    public ResponseEntity<?>  create(@RequestBody MaterialRequest material) {
        //return materialService.save(material, UUID.fromString(refreshToken));
        return materialService.save(material);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PutMapping
    public ResponseEntity<String> update(@RequestBody Material material, @CookieValue("refreshToken") String refreshToken) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Jwt jwt = jwtDecoder.decode(refreshToken);
        var userUUID = tokenFromDb.get().getUser().getIdUser();

        return materialService.update(material, userUUID);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, JwtAuthenticationToken token) {
        return materialService.deleteById(id, UUID.fromString(token.getName()));
    }
}
