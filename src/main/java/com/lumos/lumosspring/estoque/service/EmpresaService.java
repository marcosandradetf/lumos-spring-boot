package com.lumos.lumosspring.estoque.service;

import com.lumos.lumosspring.estoque.model.Empresa;
import com.lumos.lumosspring.estoque.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {
    @Autowired
    private EmpresaRepository empresaRepository;

    public List<Empresa> findAll() {
        return empresaRepository.findAll();
    }

    public Empresa findById(Long id) {
        return empresaRepository.findById(id).orElse(null);
    }

    public Empresa save(Empresa material) {
        return empresaRepository.save(material);
    }

    public void deleteById(Long id) {
        empresaRepository.deleteById(id);
    }
}
