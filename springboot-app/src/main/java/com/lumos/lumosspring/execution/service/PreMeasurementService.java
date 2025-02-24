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

import java.util.*;

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
        premeasurement.setTypeMeasurement(PreMeasurement.Type.INSTALLATION);
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
        var items = itemRepository.findItemsByPreMeasurement_PreMeasurementId(premeasurement.getPreMeasurementId());


        // IMPORTANTE
        // ADICIONAR AUTOMATICAMENTE CABO E RELE
        for (Item item : items) {
            if (item.getMaterial().getMaterialType().getTypeName().equalsIgnoreCase("CABO")) {
                double cableQuantity = 0.0F;

                // Filtra os materiais do tipo "BRAÇO"
                var bracos = items.stream()
                        .filter(b -> "BRAÇO".equalsIgnoreCase(b.getMaterial().getMaterialType().getTypeName()))
                        .toList();

                // Aplica os coeficientes da fórmula
                for (Item braco : bracos) {
                    String length = braco.getMaterial().getMaterialLength();

                    if (length.contains("1,5")) cableQuantity += braco.getItemQuantity() * 2.5;

                    if (length.contains("2,5")) cableQuantity += braco.getItemQuantity() * 8.5;

                    if (length.contains("3,5"))   cableQuantity += braco.getItemQuantity() * 12.5;

                }

                item.setItemQuantity(cableQuantity);
                itemRepository.save(item);
            }
        }




        return ResponseEntity.ok().body(new DefaultResponse("Medição salva com sucesso"));
    }

    public ResponseEntity<?> getFields(long measurementId) {
        var items = itemRepository.findItemsByPreMeasurement_PreMeasurementId(measurementId);
        Map<String, Double> fields = new HashMap<>();

        for (Item item : items) {
            String description = item.getMaterial().getMaterialType().getTypeName();
            //=SUM(F7*2.5)+(G7*8.5)+(H7*12.5)
            switch (description) {
                case "LED":
                    fields.put(description.concat(" DE ").concat(item.getMaterial().getMaterialPower()), item.getItemQuantity());
                    break;
                case "BRAÇO":
                    fields.put(description.concat(" DE ").concat(item.getMaterial().getMaterialLength()), item.getItemQuantity());
                    break;
                case "CABO":
                    double cableQuantity = 0.0F;

                    // Filtra os materiais do tipo "BRAÇO"
                    var bracos = items.stream()
                            .filter(b -> "BRAÇO".equalsIgnoreCase(b.getMaterial().getMaterialType().getTypeName()))
                            .toList();

                    // Aplica os coeficientes da fórmula
                    for (Item braco : bracos) {
                        String length = braco.getMaterial().getMaterialLength();

                        if (length.contains("1,5")) cableQuantity += braco.getItemQuantity() * 2.5;

                        if (length.contains("2,5")) cableQuantity += braco.getItemQuantity() * 8.5;

                        if (length.contains("3,5"))   cableQuantity += braco.getItemQuantity() * 12.5;

                    }
                    fields.put(description, cableQuantity);
                    break;
                default:
                    fields.put(description, item.getItemQuantity());
                    break;
            }
        }

        return ResponseEntity.ok(fields);
    }


}
