package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
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

    public ResponseEntity<?> Post(MeasurementDTO dto) {

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> Update(MeasurementDTO dto) {

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> Delete(long id) {

        return ResponseEntity.ok().build();
    }

}
