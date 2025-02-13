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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Método para extrair as partes do texto e os separadores dinamicamente
    private void extractPartsAndSeparators(String original, List<String> parts, List<String> separators) {
        Matcher matcher = Pattern.compile("([^\\s,\\-]+)([\\s,\\-]*)").matcher(original);

        while (matcher.find()) {
            parts.add(matcher.group(1));        // Captura o texto
            separators.add(matcher.group(2));   // Captura o separador (incluindo espaços)
        }
    }

    // Método para reconstruir a string original
    private String reconstructAddress(List<String> parts, List<String> separators) {
        StringBuilder restored = new StringBuilder();

        for (int i = 0; i < parts.size(); i++) {
            restored.append(parts.get(i));
            if (i < separators.size()) {
                restored.append(separators.get(i)); // Adiciona o separador original
            }
        }

        return restored.toString();
    }

    public ResponseEntity<?> saveMeasurement(MeasurementDTO measurementDTO) {
        PreMeasurement premeasurement = new PreMeasurement();

        var measurement = measurementDTO.measurement();
        var deposit = depositRepository.findById(measurement.depositId());
        // Pegamos o endereço original antes do split
        var address = measurement.address();

        // Separar preservando os separadores
        List<String> parts = new ArrayList<>();
        List<String> separators = new ArrayList<>();

        extractPartsAndSeparators(address, parts, separators);

        // Adicionamos o número na primeira parte do endereço, se necessário
        if (!measurement.number().isEmpty() && !parts.isEmpty()) {
            parts.set(0, parts.getFirst().trim().concat(", ").concat(measurement.number()));
            address = reconstructAddress(parts, separators);
        }

        premeasurement.setAddress(address);

        premeasurement.setCity(measurement.city());
        premeasurement.setLatitude(measurement.latitude());
        premeasurement.setLongitude(measurement.longitude());
        premeasurement.setDeposit(deposit.orElse(null));

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
