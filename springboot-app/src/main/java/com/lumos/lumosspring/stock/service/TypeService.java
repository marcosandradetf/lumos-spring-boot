package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.stock.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeService {
    @Autowired
    private TypeRepository tipoRepository;

    public List<Type> findAll() {
        return tipoRepository.findAll();
    }

    public Type findById(Long id) {
        return tipoRepository.findById(id).orElse(null);
    }

    public Type save(Type material) {
        return tipoRepository.save(material);
    }

    public void deleteById(Long id) {
        tipoRepository.deleteById(id);
    }
}
