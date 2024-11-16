package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepositService {
    @Autowired
    private DepositRepository depositRepository;

    public List<Deposit> findAll() {
        return depositRepository.findAll();
    }

    public Deposit findById(Long id) {
        return depositRepository.findById(id).orElse(null);
    }

    public Deposit save(Deposit material) {
        return depositRepository.save(material);
    }

    public void deleteById(Long id) {
        depositRepository.deleteById(id);
    }
}
