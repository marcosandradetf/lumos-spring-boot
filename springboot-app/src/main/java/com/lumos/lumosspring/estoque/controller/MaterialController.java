package com.lumos.lumosspring.estoque.controller;

import com.lumos.lumosspring.estoque.model.Material;
import com.lumos.lumosspring.estoque.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/material")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<Material> getAll() {
        return materialService.findAll();
    }

    @GetMapping("/{id}")
    public Material getById(@PathVariable Long id) {
        return materialService.findById(id);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('SCOPE_MANAGER')")
    @PostMapping
    public ResponseEntity<String>  create(@RequestBody Material material, JwtAuthenticationToken token) {
        return materialService.save(material, UUID.fromString(token.getName()));
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
