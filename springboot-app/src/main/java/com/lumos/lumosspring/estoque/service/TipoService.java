package com.lumos.lumosspring.estoque.service;

import com.lumos.lumosspring.estoque.model.Tipo;
import com.lumos.lumosspring.estoque.repository.TipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoService {
    @Autowired
    private TipoRepository tipoRepository;

    public List<Tipo> findAll() {
        return tipoRepository.findAll();
    }

    public Tipo findById(Long id) {
        return tipoRepository.findById(id).orElse(null);
    }

    public Tipo save(Tipo material) {
        return tipoRepository.save(material);
    }

    public void deleteById(Long id) {
        tipoRepository.deleteById(id);
    }
}
