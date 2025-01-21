package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PreMeasurementService {
    private final PreMeasurementRepository repository;

    public PreMeasurementService(PreMeasurementRepository repository) {
        this.repository = repository;
    }

    public ResponseEntity<?> GetAll() {

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> Post(PreMeasurementDTO dto) {

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> Update(PreMeasurementDTO dto) {

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> Delete(long id) {

        return ResponseEntity.ok().build();
    }

}
