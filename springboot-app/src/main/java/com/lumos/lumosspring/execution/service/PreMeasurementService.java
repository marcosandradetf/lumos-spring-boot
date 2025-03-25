package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.execution.dto.response.PreMeasurementStreetItemResponseDTO;
import com.lumos.lumosspring.execution.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.dto.response.PreMeasurementResponseDTO;
import com.lumos.lumosspring.execution.dto.response.PreMeasurementStreetResponseDTO;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreet;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItemService;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementStreetItemRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementStreetItemServiceRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementStreetRepository;
import com.lumos.lumosspring.stock.entities.MaterialService;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.stock.repository.ServiceRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Util;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PreMeasurementService {
    private final PreMeasurementStreetRepository preMeasurementStreetRepository;
    private final MaterialRepository materialRepository;
    private final PreMeasurementRepository preMeasurementRepository;
    private final PreMeasurementStreetItemRepository preMeasurementStreetItemRepository;
    private final PreMeasurementStreetItemServiceRepository preMeasurementStreetItemServiceRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final Util util;

    public PreMeasurementService(PreMeasurementStreetRepository preMeasurementStreetRepository, MaterialRepository materialRepository, PreMeasurementRepository preMeasurementRepository, PreMeasurementStreetItemRepository preMeasurementStreetItemRepository, PreMeasurementStreetItemServiceRepository preMeasurementStreetItemServiceRepository, ServiceRepository serviceRepository, UserRepository userRepository, Util util) {
        this.preMeasurementStreetRepository = preMeasurementStreetRepository;
        this.materialRepository = materialRepository;
        this.preMeasurementRepository = preMeasurementRepository;
        this.preMeasurementStreetItemRepository = preMeasurementStreetItemRepository;
        this.preMeasurementStreetItemServiceRepository = preMeasurementStreetItemServiceRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.util = util;
    }


    public ResponseEntity<?> saveMeasurement(PreMeasurementDTO measurementDTO, String userUUID) {
        if (userUUID == null || userUUID.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user UUID is required"));
        }

        var measurement = measurementDTO.measurement();
        var user = userRepository.findByIdUser(UUID.fromString(userUUID));

        PreMeasurement preMeasurement = preMeasurementRepository.getTopByCityAndStatusOrderByCreatedAtDesc(
                        measurement.city(), PreMeasurement.Status.PENDING)
                .orElse(null);
        PreMeasurementStreet preMeasurementStreet = new PreMeasurementStreet();

        if (preMeasurement == null) {
            preMeasurement = new PreMeasurement();
            preMeasurement.setCreatedBy(user.orElse(null));
            preMeasurement.setCreatedAt(util.getDateTime());
            preMeasurement.setTypePreMeasurement(PreMeasurement.Type.INSTALLATION);
            preMeasurement.setStatus(PreMeasurement.Status.PENDING);
            preMeasurement.setCity(measurement.city());
            preMeasurementRepository.save(preMeasurement);
        }

        preMeasurementStreet.setPreMeasurement(preMeasurement);
        preMeasurementStreet.setAddress(measurement.address());
        preMeasurementStreet.setStreet(measurement.street());
        preMeasurementStreet.setLatitude(measurement.latitude());
        preMeasurementStreet.setLongitude(measurement.longitude());
        preMeasurementStreet.setLastPower(measurement.lastPower());
        preMeasurementStreet.setStatus(PreMeasurementStreet.Status.PENDING);
        preMeasurementStreetRepository.save(preMeasurementStreet);


        // IMPORTANTE
        // ADICIONAR AUTOMATICAMENTE CABO E RELE
        measurementDTO.items().forEach(item -> {
            materialRepository.findById(Long.valueOf(item.materialId())).ifPresent(material -> {
                var newItem = new PreMeasurementStreetItem();
                newItem.setMaterial(material);
                newItem.setItemQuantity(item.materialQuantity());
                preMeasurementStreet.addItem(newItem);
                preMeasurementStreetItemRepository.save(newItem);

                if (material.getMaterialType().getTypeName().toUpperCase().startsWith("LED")) {
                    var ledService = serviceRepository
                            .findByServiceName("SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED", Limit.of(1));
                    if (ledService.isEmpty()){
                        ledService = Optional.of(new MaterialService());
                        ledService.get().setServiceName("SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED");
                        serviceRepository.save(ledService.get());
                    }

                    var projectForPi = serviceRepository
                            .findByServiceName("SERVIÇO DE EXECUÇÃO DE PROJETO POR IP", Limit.of(1));
                    if (projectForPi.isEmpty()) {
                        projectForPi = Optional.of(new MaterialService());
                        projectForPi.get().setServiceName("SERVIÇO DE EXECUÇÃO DE PROJETO POR IP");
                        serviceRepository.save(projectForPi.get());
                    }

                    var itemService = new PreMeasurementStreetItemService();
                    itemService.addServiceQuantity(item.materialQuantity());
                    itemService.setService(ledService.get());
                    newItem.addService(itemService);

                    itemService.addServiceQuantity(item.materialQuantity());
                    itemService.setService(projectForPi.get());
                    newItem.addService(itemService);
                } else if (material.getMaterialType().getTypeName().toUpperCase().startsWith("BRA")) {
                    var armService = serviceRepository
                            .findByServiceName("SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS", Limit.of(1));
                    if (armService.isEmpty()){
                        armService = Optional.of(new MaterialService());
                        armService.get().setServiceName("SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS");
                        serviceRepository.save(armService.get());
                    }

                    var itemService = new PreMeasurementStreetItemService();
                    itemService.addServiceQuantity(item.materialQuantity());
                    itemService.setService(armService.get());
                    newItem.addService(itemService);
                }



                material.getRelatedMaterials().stream()
                        .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("RELE"))
                        .findFirst().ifPresent(rm -> {
                            preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet).stream()
                                    .filter(i -> i.getMaterial().getIdMaterial() == rm.getIdMaterial())
                                    .findFirst().ifPresentOrElse(
                                            existingRelay -> {
                                                existingRelay.addItemQuantity(item.materialQuantity());
                                                preMeasurementStreetItemRepository.save(existingRelay);
                                            },
                                            () -> {
                                                var newRelay = new PreMeasurementStreetItem();
                                                newRelay.setMaterial(rm);
                                                newRelay.addItemQuantity(item.materialQuantity());
                                                preMeasurementStreet.addItem(newRelay);
                                                preMeasurementStreetItemRepository.save(newRelay);
                                            }
                                    );
                        });

                material.getRelatedMaterials().stream()
                        .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("CABO"))
                        .findFirst().ifPresent(rm -> {
                            preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet).stream()
                                    .filter(i -> i.getMaterial().getIdMaterial() == rm.getIdMaterial())
                                    .findFirst().ifPresentOrElse(
                                            existingCable -> {
                                                if (material.getMaterialLength().startsWith("1"))
                                                    existingCable.addItemQuantity(item.materialQuantity() * 2.5);
                                                if (material.getMaterialLength().startsWith("2"))
                                                    existingCable.addItemQuantity(item.materialQuantity() * 8.5);
                                                if (material.getMaterialLength().startsWith("3"))
                                                    existingCable.addItemQuantity(item.materialQuantity() * 12.5);
                                                preMeasurementStreetItemRepository.save(existingCable);
                                            },
                                            () -> {
                                                var newCable = new PreMeasurementStreetItem();
                                                newCable.setMaterial(rm);
                                                if (material.getMaterialLength().startsWith("1"))
                                                    newCable.addItemQuantity(item.materialQuantity() * 2.5);
                                                if (material.getMaterialLength().startsWith("2"))
                                                    newCable.addItemQuantity(item.materialQuantity() * 8.5);
                                                if (material.getMaterialLength().startsWith("3"))
                                                    newCable.addItemQuantity(item.materialQuantity() * 12.5);
                                                preMeasurementStreet.addItem(newCable);
                                                preMeasurementStreetItemRepository.save(newCable);
                                            }
                                    );
                        });
            });
        });


        return ResponseEntity.ok().body(new DefaultResponse("Medição salva com sucesso"));
    }


    public ResponseEntity<?> getAll(PreMeasurement.Status status) {
        List<PreMeasurementResponseDTO> measurements = preMeasurementRepository
                .findAllByStatusOrderByCreatedAtAsc(status)
                .stream()
                .sorted(Comparator.comparing(PreMeasurement::getPreMeasurementId))
                .map(p -> new PreMeasurementResponseDTO(
                        p.getPreMeasurementId(),
                        p.getCity(),
                        p.getCreatedBy() != null ? p.getCreatedBy().getCompletedName() : "Desconhecido",
                        util.normalizeDate(p.getCreatedAt()),
                        "",
                        p.getTypePreMeasurement().name(),
                        p.getTypePreMeasurement() == PreMeasurement.Type.INSTALLATION ?
                                "badge-primary" : "badge-neutral",
                        p.getTypePreMeasurement().name(),
                        p.getTotalPrice() != null ? p.getTotalPrice().toString() : "0,00",
                        p.getStreets().stream()
                                .sorted(Comparator.comparing(PreMeasurementStreet::getPreMeasurementStreetId))
                                .map(s -> new PreMeasurementStreetResponseDTO(
                                        s.getPreMeasurementStreetId(),
                                        s.getLastPower(),
                                        s.getLatitude(),
                                        s.getLongitude(),
                                        s.getStreet(),
                                        s.getItems() != null ? s.getItems().stream()
                                                .sorted(Comparator.comparing(PreMeasurementStreetItem::getPreMeasurementStreetItemId))
                                                .map(i -> new PreMeasurementStreetItemResponseDTO(
                                                        i.getPreMeasurementStreetItemId(),
                                                        i.getMaterial().getIdMaterial(),
                                                        i.getMaterial().getMaterialName(),
                                                        i.getMaterial().getMaterialType().getTypeName(),
                                                        i.getMaterial().getMaterialPower(),
                                                        i.getMaterial().getMaterialLength(),
                                                        i.getItemQuantity()
                                                )).toList()
                                                : List.of() // Retorna lista vazia se `s.getItems()` for null
                                )).toList()
                )).toList();

        return ResponseEntity.ok().body(measurements);
    }


    public ResponseEntity<?> getPreMeasurement(long preMeasurementId) {
        PreMeasurement p = preMeasurementRepository
                .findByPreMeasurementIdAndStatus(preMeasurementId, PreMeasurement.Status.VALIDATING);

        var preMeasurement = new PreMeasurementResponseDTO(
                p.getPreMeasurementId(),
                p.getCity(),
                p.getCreatedBy() != null ? p.getCreatedBy().getCompletedName() : "Desconhecido",
                util.normalizeDate(p.getCreatedAt()),
                "",
                p.getTypePreMeasurement().name(),
                p.getTypePreMeasurement() == PreMeasurement.Type.INSTALLATION ?
                        "badge-primary" : "badge-neutral",
                p.getTypePreMeasurement().name(),
                p.getTotalPrice() != null ? p.getTotalPrice().toString() : "0,00",
                p.getStreets().stream()
                        .sorted(Comparator.comparing(PreMeasurementStreet::getPreMeasurementStreetId))
                        .map(s -> new PreMeasurementStreetResponseDTO(
                                s.getPreMeasurementStreetId(),
                                s.getLastPower(),
                                s.getLatitude(),
                                s.getLongitude(),
                                s.getStreet(),
                                s.getItems() != null ? s.getItems().stream()
                                        .sorted(Comparator.comparing(PreMeasurementStreetItem::getPreMeasurementStreetItemId))
                                        .map(i -> new PreMeasurementStreetItemResponseDTO(
                                                i.getPreMeasurementStreetItemId(),
                                                i.getMaterial().getIdMaterial(),
                                                i.getMaterial().getMaterialName(),
                                                i.getMaterial().getMaterialType().getTypeName(),
                                                i.getMaterial().getMaterialPower(),
                                                i.getMaterial().getMaterialLength(),
                                                i.getItemQuantity()
                                        )).toList()
                                        : List.of() // Retorna lista vazia se `s.getItems()` for null
                        )).toList()
        );

        return ResponseEntity.ok().body(preMeasurement);
    }
}
