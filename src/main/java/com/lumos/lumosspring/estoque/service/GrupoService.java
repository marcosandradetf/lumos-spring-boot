package com.lumos.lumosspring.estoque.service;

import com.lumos.lumosspring.estoque.model.Grupo;
import com.lumos.lumosspring.estoque.repository.GrupoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrupoService {
    @Autowired
    private GrupoRepository grupoRepository;

    public List<Grupo> findAll() {
        return grupoRepository.findAll();
    }

    public Grupo findById(Long id) {
        return grupoRepository.findById(id).orElse(null);
    }

    public Grupo save(Grupo material) {
        return grupoRepository.save(material);
    }

    public void deleteById(Long id) {
        grupoRepository.deleteById(id);
    }
}
