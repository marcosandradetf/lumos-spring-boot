package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.ItemsDTO;
import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.controller.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeasurementService {
    private final ItemRepository itemRepository;

    public MeasurementService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public ResponseEntity<?> getAll() {
        List<MeasurementDTO> measurements = itemRepository.findItemsByMeasurement_Status(PreMeasurement.Status.PENDING)
                .stream()
                .collect(Collectors.groupingBy(Item::getMeasurement)) // Group items by their measurement
                .entrySet()
                .stream()
                .map(entry -> new MeasurementDTO(
                        new PreMeasurementDTO(
                                entry.getKey().getMeasurementId(),
                                entry.getKey().getLatitude(),
                                entry.getKey().getLongitude(),
                                entry.getKey().getAddress(),
                                entry.getKey().getCity(),
                                entry.getKey().getDeposit().getIdDeposit(),
                                entry.getKey().getDeviceId(),
                                entry.getKey().getDeposit().getDepositName(),
                                entry.getKey().getTypeMeasurement().name(),
                                entry.getKey().getTypeMeasurement() == PreMeasurement.Type.INSTALLATION ?
                                        "badge-primary" : "badge-neutral",
                                entry.getKey().getCreatedBy().getCompletedName()

                        ),
                        entry.getValue().stream()
                                .map(i -> new ItemsDTO(
                                        i.getItemId(),
                                        String.valueOf(i.getMaterial().getIdMaterial()), // No need to cast
                                        ((int) i.getItemQuantity()), // No need to cast
                                        "",
                                        i.getMeasurement().getMeasurementId(),
                                        i.getMaterial().getMaterialName()
                                ))
                                .collect(Collectors.toList()) // Collect items into a list
                ))
                .toList();

        return ResponseEntity.ok().body(measurements);
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
