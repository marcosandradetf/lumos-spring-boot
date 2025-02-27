package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.controller.dto.ItemsResponse;
import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.repository.ItemRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.repository.StreetRepository;
import com.lumos.lumosspring.stock.entities.MaterialStock;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.stock.repository.ProductStockRepository;
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
    private final ProductStockRepository materialStockRepository;
    private final DepositRepository depositRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Util util;

    public PreMeasurementService(PreMeasurementRepository preMeasurementRepository, ItemRepository repository, StreetRepository streetRepository, MaterialRepository materialRepository, ProductStockRepository materialStockRepository, DepositRepository depositRepository, ItemRepository itemRepository, UserRepository userRepository, Util util) {
        this.preMeasurementRepository = preMeasurementRepository;
        this.materialRepository = materialRepository;
        this.materialStockRepository = materialStockRepository;
        this.depositRepository = depositRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.util = util;
    }


    public ResponseEntity<?> getItems(Long depositId) {
        List<MaterialStock> materials = materialStockRepository.getByDeposit(depositId);
        List<ItemsResponse> items = new ArrayList<>();

        for (MaterialStock m : materials) {
            items.add(new ItemsResponse(
                    m.getMaterial().getIdMaterial(),
                    m.getMaterial().getMaterialName(),
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
            var material = materialStockRepository.findById(Long.valueOf(item.materialId()));
            newItem.setMaterialStock(material.orElse(null));
            newItem.setItemQuantity(item.materialQuantity());
            newItem.setMeasurement(premeasurement);

            itemRepository.save(newItem);
        });
        var items = itemRepository.findItemsByPreMeasurement_PreMeasurementId(premeasurement.getPreMeasurementId());


        // IMPORTANTE
        // ADICIONAR AUTOMATICAMENTE CABO E RELE
        for (Item item : items) {
            var material = item.getMaterialStock().getMaterial();
            if (material.getMaterialType().getTypeName().equalsIgnoreCase("CABO")) {
                double cableQuantity = 0.0F;

                for (Item i : items) {
                    String braco = i.getMaterialStock().getMaterial().getMaterialType().getTypeName().toUpperCase();
                    if (braco.equalsIgnoreCase("BRAÇO 1,5")) cableQuantity += i.getItemQuantity() * 2.5;

                    if (braco.equalsIgnoreCase("BRAÇO 2,5")) cableQuantity += i.getItemQuantity() * 8.5;

                    if (braco.equalsIgnoreCase("BRAÇO 3,5")) cableQuantity += i.getItemQuantity() * 12.5;
                }

                item.setItemQuantity(cableQuantity);
                itemRepository.save(item);
            }

            if (material.getMaterialType().getTypeName().equalsIgnoreCase("RELÉ")) {
                double releQuantity = 0.0F;

                for (Item i : items) {
                    String braco = i.getMaterialStock().getMaterial().getMaterialType().getTypeName().toUpperCase();
                    if (braco.equalsIgnoreCase("LED")) releQuantity += i.getItemQuantity();
                }

                item.setItemQuantity(releQuantity);
                itemRepository.save(item);
            }
        }


        return ResponseEntity.ok().body(new DefaultResponse("Medição salva com sucesso"));
    }

    public ResponseEntity<?> getFields(long measurementId) {
        var items = itemRepository.findItemsByPreMeasurement_PreMeasurementId(measurementId);

        // Record para armazenar os dados de um item
        record ItemField(String description, double quantity) {
        }

        // Mapa para armazenar os itens por categoria
        Map<String, List<ItemField>> itemsFields = new HashMap<>();

        for (Item item : items) {
            var material = item.getMaterialStock().getMaterial();
            String type = material.getMaterialType().getTypeName().toUpperCase();
            String description;

            switch (type) {
                case "LED":
                    description = "LED DE ".concat(material.getMaterialPower());
                    itemsFields.computeIfAbsent("leds", k -> new ArrayList<>())
                            .add(new ItemField(description, item.getItemQuantity()));
                    break;
                case "BRAÇO":
                    description = "BRAÇO DE ".concat(material.getMaterialLength());
                    itemsFields.computeIfAbsent("arms", k -> new ArrayList<>())
                            .add(new ItemField(description, item.getItemQuantity()));
                    break;
                case "PARAFUSO":
                    itemsFields.computeIfAbsent("screws", k -> new ArrayList<>())
                            .add(new ItemField("PARAFUSO", item.getItemQuantity()));
                    break;
                case "CINTA":
                    itemsFields.computeIfAbsent("straps", k -> new ArrayList<>())
                            .add(new ItemField("CINTA", item.getItemQuantity()));
                    break;
                case "RELÉ":
                    itemsFields.computeIfAbsent("relays", k -> new ArrayList<>())
                            .add(new ItemField("RELÉ", item.getItemQuantity()));
                    break;
                case "SOQUETE":
                    itemsFields.computeIfAbsent("sockets", k -> new ArrayList<>())
                            .add(new ItemField("PERFURANTE", item.getItemQuantity()));
                    break;
                default:
                    break;
            }
        }

        return ResponseEntity.ok(itemsFields);
    }


}
