package com.lumos.lumosspring.estoque.service;

import com.lumos.lumosspring.estoque.model.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lumos.lumosspring.estoque.repository.MaterialRepository;

import java.util.List;

@Service
public class MaterialService {
    @Autowired
    private MaterialRepository materialRepository;

    public List<Material> findAll() {
        return materialRepository.findAll();
    }

    public Material findById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    public Material save(Material material) {
        if (materialRepository.existsByName(material.getNomeMaterial())) {
            throw new IllegalArgumentException("Material já cadastrado.");
        }
        return materialRepository.save(material);
    }

    public void deleteById(Long id) {
        materialRepository.deleteById(id);
    }
}
