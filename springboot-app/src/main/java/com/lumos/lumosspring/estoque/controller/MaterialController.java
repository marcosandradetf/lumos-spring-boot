package com.lumos.lumosspring.estoque.controller;

import com.lumos.lumosspring.estoque.controller.dto.MaterialRequest;
import com.lumos.lumosspring.estoque.controller.dto.MaterialResponse;
import com.lumos.lumosspring.estoque.model.Material;
import com.lumos.lumosspring.estoque.repository.MaterialRepository;
import com.lumos.lumosspring.estoque.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/material")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialRepository materialRepository;

    public MaterialController(MaterialService materialService, MaterialRepository materialRepository) {
        this.materialService = materialService;
        this.materialRepository = materialRepository;
    }

    // Endpoint para retornar todos os materiais
    @GetMapping
    public ResponseEntity<List<MaterialResponse>> getAllMateriais() {
        // Recupera todos os materiais do banco de dados
        List<Material> materiais = materialRepository.findAll();

        // Converte cada material em MaterialDTO
        List<MaterialResponse> materiaisDTO = materiais.stream()
                .map(MaterialResponse::new)  // Chama o construtor do DTO que recebe a entidade Material
                .collect(Collectors.toList());

        return ResponseEntity.ok(materiaisDTO);
    }

    @GetMapping("/{id}")
    public Material getById(@PathVariable Long id) {
        return materialService.findById(id);
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
    public ResponseEntity<String> update(@RequestBody Material material, JwtAuthenticationToken token) {
        return materialService.update(material, UUID.fromString(token.getName()));
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, JwtAuthenticationToken token) {
        return materialService.deleteById(id, UUID.fromString(token.getName()));
    }
}
