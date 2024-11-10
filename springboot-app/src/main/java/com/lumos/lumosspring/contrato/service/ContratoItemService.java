package com.lumos.lumosspring.contrato.service;

import com.lumos.lumosspring.contrato.repository.ContratoItensRepository;
import org.springframework.stereotype.Service;

@Service
public class ContratoItemService {
    private final ContratoItensRepository repository;

    public ContratoItemService(ContratoItensRepository repository) {
        this.repository = repository;
    }


}
