package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.ItemsResponse;
import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.repository.StreetRepository;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Util;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PreMeasurementService {
    private final PreMeasurementRepository preMeasurementRepository;
    private final MaterialRepository materialRepository;
    private final DepositRepository depositRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Util util;

    public PreMeasurementService(PreMeasurementRepository preMeasurementRepository, ItemRepository repository, StreetRepository streetRepository, MaterialRepository materialRepository, DepositRepository depositRepository, ItemRepository itemRepository, UserRepository userRepository, Util util) {
        this.preMeasurementRepository = preMeasurementRepository;
        this.materialRepository = materialRepository;
        this.depositRepository = depositRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.util = util;
    }


    public ResponseEntity<?> getItems(Long depositId) {
        List<Material> materials = materialRepository.getByDeposit(depositId);
        List<ItemsResponse> items = new ArrayList<>();

        for (Material m : materials) {
            items.add(new ItemsResponse(
                    m.getIdMaterial(),
                    m.getMaterialName(),
                    m.getStockQuantity()
            ));
        }

        return ResponseEntity.ok(items);
    }


    public ResponseEntity<?> saveMeasurement(MeasurementDTO measurementDTO, String userUUID) {
        if (userUUID == null || userUUID.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user UUID is required"));
        }

        PreMeasurement premeasurement = new PreMeasurement();

        var measurement = measurementDTO.measurement();
        var deposit = depositRepository.findById(measurement.depositId());
        var user = userRepository.findByIdUser(UUID.fromString(userUUID));

        premeasurement.setAddress(measurement.address());
        premeasurement.setCity(measurement.city());
        premeasurement.setLatitude(measurement.latitude());
        premeasurement.setLongitude(measurement.longitude());
        premeasurement.setDeposit(deposit.orElse(null));
        premeasurement.setStatus(PreMeasurement.Status.PENDING);
        premeasurement.setCreatedBy(user.orElse(null));
        premeasurement.setCreatedAt(util.getDateTime());

        preMeasurementRepository.save(premeasurement);


        measurementDTO.items().forEach(item -> {
            var newItem = new Item();
            var material = materialRepository.findById(Long.valueOf(item.materialId()));
            newItem.setMaterial(material.orElse(null));
            newItem.setItemQuantity(item.materialQuantity());
            newItem.setMeasurement(premeasurement);

            itemRepository.save(newItem);
        });

        return ResponseEntity.ok().body(new DefaultResponse("Medição salva com sucesso"));
    }
}
