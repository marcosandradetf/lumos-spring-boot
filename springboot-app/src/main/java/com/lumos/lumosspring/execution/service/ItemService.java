package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.ItemsResponse;
import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.Street;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.repository.StreetRepository;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.util.DefaultResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {
    private final PreMeasurementRepository preMeasurementRepository;
    private final ItemRepository repository;
    private final StreetRepository streetRepository;
    private final MaterialRepository materialRepository;
    private final DepositRepository depositRepository;
    private final ItemRepository itemRepository;

    public ItemService(PreMeasurementRepository preMeasurementRepository, ItemRepository repository, StreetRepository streetRepository, MaterialRepository materialRepository, DepositRepository depositRepository, ItemRepository itemRepository) {
        this.preMeasurementRepository = preMeasurementRepository;
        this.repository = repository;
        this.streetRepository = streetRepository;
        this.materialRepository = materialRepository;
        this.depositRepository = depositRepository;
        this.itemRepository = itemRepository;
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

    public ResponseEntity<?> saveMeasurement(MeasurementDTO measurementDTO) {
        PreMeasurement premeasurement = new PreMeasurement();

        var measurement = measurementDTO.measurement();
        var deposit = depositRepository.findById(measurement.depositId());

        premeasurement.setAddress(measurement.address());
        premeasurement.setLatitude(measurement.latitude());
        premeasurement.setLongitude(measurement.longitude());
        premeasurement.setDeposit(deposit.orElse(null));

        preMeasurementRepository.save(premeasurement);


        measurementDTO.items().forEach(item -> {
            var newItem = new Item();
            var material =  materialRepository.findById(Long.valueOf(item.materialId()));
            newItem.setMaterial(material.orElse(null));
            newItem.setItemQuantity(item.materialQuantity());
            newItem.setMeasurement(premeasurement);

            itemRepository.save(newItem);
        });

        return ResponseEntity.ok().body(new DefaultResponse("Medição salva com sucesso"));
    }
}
