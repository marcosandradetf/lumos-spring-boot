package com.lumos.lumosspring.estoque.service;

import com.lumos.lumosspring.estoque.model.Almoxarifado;
import com.lumos.lumosspring.estoque.repository.AlmoxarifadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlmoxarifadoService {
    @Autowired
    private AlmoxarifadoRepository almoxarifadoRepository;

    public List<Almoxarifado> findAll() {
        return almoxarifadoRepository.findAll();
    }

    public Almoxarifado findById(Long id) {
        return almoxarifadoRepository.findById(id).orElse(null);
    }

    public Almoxarifado save(Almoxarifado material) {
        return almoxarifadoRepository.save(material);
    }

    public void deleteById(Long id) {
        almoxarifadoRepository.deleteById(id);
    }
}
