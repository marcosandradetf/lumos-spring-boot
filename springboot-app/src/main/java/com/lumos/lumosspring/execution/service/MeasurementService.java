package com.lumos.lumosspring.execution.service;


import com.lumos.lumosspring.execution.controller.dto.MeasurementValuesDTO;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItemService;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementStreetItemRepository;
import com.lumos.lumosspring.stock.service.DepositService;
import com.lumos.lumosspring.stock.service.MaterialService;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Util;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeasurementService {
    private final PreMeasurementRepository preMeasurementRepository;
    private final PreMeasurementStreetItemRepository preMeasurementStreetItemRepository;
    private final Util util;

    public MeasurementService(PreMeasurementRepository preMeasurementRepository, PreMeasurementStreetItemRepository preMeasurementStreetItemRepository, Util util) {
        this.preMeasurementRepository = preMeasurementRepository;
        this.preMeasurementStreetItemRepository = preMeasurementStreetItemRepository;
        this.util = util;
    }


    public ResponseEntity<?> getFields(long measurementId) {
        var preMeasurement = preMeasurementRepository.findById(measurementId);
        if (preMeasurement.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var items = preMeasurementStreetItemRepository.findAllByPreMeasurementStreetItemId(measurementId);

        // Record para armazenar os dados de um item
        record ItemField(String description, double quantity) {
        }

        Map<String, Map<String, Double>> groupedItems = new HashMap<>();

        for (var street : preMeasurement.get().getStreets()) {
            for (PreMeasurementStreetItem preMeasurementStreetItem : street.getItems()) {
                var material = preMeasurementStreetItem.getMaterial();
                String type = material.getMaterialType().getTypeName().toUpperCase();
                String description;

                switch (type) {
                    case "LED":
                    case "LEDS":
                        description = material.getMaterialPower().toUpperCase();
                        groupedItems.computeIfAbsent("leds", k -> new HashMap<>())
                                .merge(description, preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        groupedItems.computeIfAbsent("ledService", k -> new HashMap<>())
                                .merge("SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        groupedItems.computeIfAbsent("piService", k -> new HashMap<>())
                                .merge("SERVIÇO DE EXECUÇÃO DE PROJETO POR IP", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    case "BRAÇO":
                    case "BRACO":
                    case "BRAÇOS":
                    case "BRACOS":
                        description = material.getMaterialLength().toUpperCase();
                        groupedItems.computeIfAbsent("arms", k -> new HashMap<>())
                                .merge(description, preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        groupedItems.computeIfAbsent("armService", k -> new HashMap<>())
                                .merge("SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    case "PARAFUSO":
                    case "PARAFUSOS":
                        groupedItems.computeIfAbsent("screws", k -> new HashMap<>())
                                .merge("PARAFUSO", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    case "CINTA":
                    case "CINTAS":
                        groupedItems.computeIfAbsent("straps", k -> new HashMap<>())
                                .merge("CINTA", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    case "RELÉ":
                    case "RELE":
                    case "RELÉS":
                    case "RELES":
                        groupedItems.computeIfAbsent("relays", k -> new HashMap<>())
                                .merge("RELÉ", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    case "CONECTOR":
                    case "CONECTORES":
                        groupedItems.computeIfAbsent("connectors", k -> new HashMap<>())
                                .merge("CONECTOR", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    case "CABO":
                    case "CABOS":
                        groupedItems.computeIfAbsent("cables", k -> new HashMap<>())
                                .merge("CABO", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                    case "POSTE":
                    case "POSTES":
                    case "POSTE ORNAMENTAL":
                        groupedItems.computeIfAbsent("posts", k -> new HashMap<>())
                                .merge("POSTE", preMeasurementStreetItem.getItemQuantity(), Double::sum);
                        break;
                    default:
                        break;
                }
            }
        }

// Converter `groupedItems` para o formato desejado
        Map<String, List<ItemField>> itemsFields = new HashMap<>();

        groupedItems.forEach((key, value) -> {
            List<ItemField> itemList = value.entrySet().stream()
                    .map(entry -> new ItemField(entry.getKey(), entry.getValue()))
                    .toList();
            itemsFields.put(key, itemList);
        });


        return ResponseEntity.ok(itemsFields);
    }

    public ResponseEntity<?> saveMeasurementValues(Map<String, List<MeasurementValuesDTO>> valuesDTO, Long preMeasurementId) {
        var preMeasurement = preMeasurementRepository.findById(preMeasurementId);
        if (preMeasurement.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            valuesDTO.forEach((category, values) -> {
                values.forEach(value -> {
                    preMeasurement.ifPresent(measurement -> {
                        measurement.getStreets().forEach(street -> {
                            street.getItems().stream()
                                    .filter(item -> {
                                        var material = item.getMaterial();
                                        return material != null && (
                                                Objects.toString(material.getMaterialLength(), "").startsWith(value.description()) ||
                                                        Objects.toString(material.getMaterialPower(), "").startsWith(value.description()) ||
                                                        Objects.equals(material.getMaterialType().getTypeName().toUpperCase(), value.description()) ||
                                                        Optional.ofNullable(item.getService(value.description())).isPresent()
                                        );
                                    })
                                    .forEach(item -> {
                                        var service = Optional.ofNullable(item.getService(value.description()));

                                        if (service.isPresent()) {
                                            service.get().setUnitPrice(util.convertToBigDecimal(value.price()));
                                        } else {
                                            item.setUnitPrice(util.convertToBigDecimal(value.price()));
                                        }

                                    });
                        });
                    });
                });
            });


            preMeasurement.get().setStatus(PreMeasurement.Status.VALIDATING);
            preMeasurementRepository.save(preMeasurement.get());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse(e.getMessage()));
        }


        return ResponseEntity.ok().body(new DefaultResponse("OK"));
    }


}
