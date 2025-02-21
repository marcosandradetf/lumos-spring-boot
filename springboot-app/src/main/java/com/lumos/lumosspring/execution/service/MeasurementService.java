package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.ItemsDTO;
import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.controller.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
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
//                                entry.getKey().getTypeMeasurement().name(),
//                                entry.getKey().getTypeMeasurement() == PreMeasurement.Type.INSTALLATION ?
//                                        "badge-primary" : "badge-neutral",
                                PreMeasurement.Type.INSTALLATION.name(),
                                "badge-primary",
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

    public ResponseEntity<?> getCities() {
        // Obtenha a lista de cidades
        var cities = itemRepository.findCities(PreMeasurement.Status.PENDING);

        // Mapeamento para armazenar os totais de cada cidade
        Map<String, List<String>> citiesMap = new HashMap<>();  // Para armazenar como List<String>

        // Iterar sobre as cidades para obter os totais
        for (String city : cities) {
            // Obtenha os totais para a cidade
            var totals = itemRepository.getTotalByCity(PreMeasurement.Status.PENDING, city);

            // Aqui assume-se que `totals` retorne uma lista de String com valores como "10.0,4"
            String totalStr = totals.getFirst();  // Pega o primeiro item da lista de totais (esperado como String)

            // Converte a string com valores separados por vírgula para um array
            String[] totalsArray = totalStr.split(",");

            // Armazena o array convertido como uma lista dentro do mapa
            List<String> totalsList = Arrays.asList(totalsArray);
            citiesMap.put(city, totalsList);  // Armazena no mapa, com a cidade como chave e a lista de totais como valor
        }

        return ResponseEntity.ok(citiesMap);
    }

}
