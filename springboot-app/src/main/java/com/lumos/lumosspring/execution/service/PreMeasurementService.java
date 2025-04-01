package com.lumos.lumosspring.execution.service;

import com.lumos.lumosspring.contract.repository.ContractRepository;
import com.lumos.lumosspring.execution.dto.response.PreMeasurementStreetItemResponseDTO;
import com.lumos.lumosspring.execution.dto.PreMeasurementDTO;
import com.lumos.lumosspring.execution.dto.response.PreMeasurementResponseDTO;
import com.lumos.lumosspring.execution.dto.response.PreMeasurementStreetResponseDTO;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.execution.entities.PreMeasurementStreet;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementStreetItemRepository;
import com.lumos.lumosspring.execution.repository.PreMeasurementStreetRepository;
import com.lumos.lumosspring.notification.service.NotificationService;
import com.lumos.lumosspring.notification.service.Routes;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.user.Role;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.*;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PreMeasurementService {
    private final PreMeasurementStreetRepository preMeasurementStreetRepository;
    private final MaterialRepository materialRepository;
    private final PreMeasurementRepository preMeasurementRepository;
    private final PreMeasurementStreetItemRepository preMeasurementStreetItemRepository;
    private final UserRepository userRepository;
    private final Util util;
    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    public PreMeasurementService(PreMeasurementStreetRepository preMeasurementStreetRepository, MaterialRepository materialRepository, PreMeasurementRepository preMeasurementRepository, PreMeasurementStreetItemRepository preMeasurementStreetItemRepository, UserRepository userRepository, Util util, ContractRepository contractRepository, NotificationService notificationService) {
        this.preMeasurementStreetRepository = preMeasurementStreetRepository;
        this.materialRepository = materialRepository;
        this.preMeasurementRepository = preMeasurementRepository;
        this.preMeasurementStreetItemRepository = preMeasurementStreetItemRepository;
        this.userRepository = userRepository;
        this.util = util;
        this.contractRepository = contractRepository;
        this.notificationService = notificationService;
    }


    @Transactional
    public ResponseEntity<?> saveMeasurement(PreMeasurementDTO preMeasurementDTO, String userUUID) {
        if (userUUID == null || userUUID.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user UUID is required"));
        }

        var contract = contractRepository.findContractByContractId(preMeasurementDTO.getContractId()).orElse(null);
        if (contract == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Contract not found"));
        }

        var user = userRepository.findByIdUser(UUID.fromString(userUUID));
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user not found"));
        }

        if (!contract.getStatus().equals(ContractStatus.PENDING)) {
            notificationService.sendNotificationForRole(
                    "Pré-medição de ".concat(Objects.requireNonNull(contract.getContractor())),
                    "Sua pré-medição não foi salva, pois já foi enviada anteriormente pelo usuário ".concat(user.get().getName()).concat(" em caso de dúvidas, procure sua empresa para evitar inconsistências na pré-medição."),
                    Routes.PRE_MEASUREMENT_PROGRESS.concat("/").concat(String.valueOf(contract.getContractId())),
                    Role.Values.RESPONSAVEL_TECNICO,
                    util.getDateTime(),
                    NotificationType.WARNING
            );
            return ResponseEntity.badRequest().body(new ErrorResponse("Já foi enviada uma pré-medição anterior para este contrato."));
        }

        PreMeasurement preMeasurement = new PreMeasurement();
        var streets = preMeasurementDTO.getStreets();

        preMeasurement.setContract(contract);
        preMeasurement.setCreatedBy(user.orElse(null));
        preMeasurement.setCreatedAt(util.getDateTime());
        preMeasurement.setTypePreMeasurement(ContractType.INSTALLATION);
        preMeasurement.setStatus(ContractStatus.PENDING);
        preMeasurement.setCity(contract.getContractor());
        preMeasurementRepository.save(preMeasurement);

        streets.forEach(streetOff -> {
            PreMeasurementStreet preMeasurementStreet = new PreMeasurementStreet();
            var street = streetOff.getStreet();

            preMeasurementStreet.setPreMeasurement(preMeasurement);
            preMeasurementStreet.setStreet(street.getStreet());
            preMeasurementStreet.setNumber(street.getNumber());
            preMeasurementStreet.setNeighborhood(street.getNeighborhood());
            preMeasurementStreet.setCity(street.getCity());
            preMeasurementStreet.setState(street.getState());
            preMeasurementStreet.setLatitude(street.getLatitude());
            preMeasurementStreet.setLongitude(street.getLongitude());
            preMeasurementStreet.setLastPower(street.getLastPower());
            preMeasurementStreet.setStreetStatus(ItemStatus.PENDING);
            preMeasurementStreetRepository.save(preMeasurementStreet);
            streetOff.getItems().forEach(item -> {
                // IMPORTANTE
                // ADICIONAR AUTOMATICAMENTE CABO E RELE
                materialRepository.findByIdWithGraphType(item.getMaterialId()).ifPresent(material -> {
                    var contractItem = contract.getContractItemsQuantitative().stream()
                            .filter(i -> Objects.equals(i.getReferenceItem().getType(), material.getMaterialType().getTypeName()))
                            .filter(i -> {
                                var linking = i.getReferenceItem().getLinking();
                                return linking == null || Objects.equals(linking, material.getMaterialPower()) || Objects.equals(linking, material.getMaterialLength());
                            })
                            .findFirst();

                    if(contractItem.isEmpty()) {
                        throw new RuntimeException("Item do Contrato não encontrado");
                    }

                    var newItem = new PreMeasurementStreetItem();
                    newItem.setPreMeasurementStreet(preMeasurementStreet);
                    newItem.setMaterial(material);
                    newItem.setItemStatus(ItemStatus.PENDING);
                    newItem.setItemQuantity(item.getMaterialQuantity());
                    newItem.setContractItem(contractItem.get());
                    if(Objects.equals(material.getMaterialType().getTypeName(), "LED")) {
                        contract.getContractItemsQuantitative().stream()
                                .filter(i -> Objects.equals(i.getReferenceItem().getType(), "SERVIÇO"))
                                .filter(i -> Objects.equals(i.getReferenceItem().getItemDependency(), "LED"))
                                .forEach(i -> {
                                    newItem.getPreMeasurementStreet().getPreMeasurement().sumTotalPrice(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getContractedQuantity())));
                                });

                    } else if (Objects.equals(material.getMaterialType().getTypeName(), "BRAÇO")) {
                        contract.getContractItemsQuantitative().stream()
                                .filter(i -> Objects.equals(i.getReferenceItem().getType(), "SERVIÇO"))
                                .filter(i -> Objects.equals(i.getReferenceItem().getItemDependency(), "BRAÇO"))
                                .forEach(i -> {
                                    newItem.getPreMeasurementStreet().getPreMeasurement().sumTotalPrice(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getContractedQuantity())));
                                });
                    }
                    preMeasurementStreetItemRepository.save(newItem);

                    material.getRelatedMaterials().stream()
                            .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("REL"))
                            .findFirst().ifPresent(rm -> {
                                preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet).stream()
                                        .filter(i -> i.getMaterial().getIdMaterial() == rm.getIdMaterial())
                                        .findFirst().ifPresentOrElse(
                                                existingRelay -> {
                                                    existingRelay.addItemQuantity(item.getMaterialQuantity());
                                                    preMeasurementStreetItemRepository.save(existingRelay);
                                                },
                                                () -> {
                                                    var newRelay = new PreMeasurementStreetItem();
                                                    var contractRelay = contract.getContractItemsQuantitative().stream()
                                                            .filter(i -> Objects.equals(i.getReferenceItem().getType(), material.getMaterialType().getTypeName()))
                                                            .filter(i -> {
                                                                var linking = i.getReferenceItem().getLinking();
                                                                return linking == null || Objects.equals(linking, material.getMaterialPower()) || Objects.equals(linking, material.getMaterialLength());
                                                            })
                                                            .findFirst();
                                                    if(contractRelay.isEmpty()) {
                                                        throw new RuntimeException("Relé do Contrato não encontrado");
                                                    }
                                                    newRelay.setMaterial(rm);
                                                    newRelay.addItemQuantity(item.getMaterialQuantity());
                                                    newRelay.setContractItem(contractRelay.get());
                                                    preMeasurementStreet.addItem(newRelay);
                                                    preMeasurementStreetItemRepository.save(newRelay);
                                                }
                                        );
                            });

                    material.getRelatedMaterials().stream()
                            .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("CAB"))
                            .findFirst().ifPresent(rm -> {
                                preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet).stream()
                                        .filter(i -> i.getMaterial().getIdMaterial() == rm.getIdMaterial())
                                        .findFirst().ifPresentOrElse(
                                                existingCable -> {
                                                    if (material.getMaterialLength().startsWith("1"))
                                                        existingCable.addItemQuantity(item.getMaterialQuantity() * 2.5);
                                                    if (material.getMaterialLength().startsWith("2"))
                                                        existingCable.addItemQuantity(item.getMaterialQuantity() * 8.5);
                                                    if (material.getMaterialLength().startsWith("3"))
                                                        existingCable.addItemQuantity(item.getMaterialQuantity() * 12.5);
                                                    preMeasurementStreetItemRepository.save(existingCable);
                                                },
                                                () -> {
                                                    var newCable = new PreMeasurementStreetItem();
                                                    var contractCable = contract.getContractItemsQuantitative().stream()
                                                            .filter(i -> Objects.equals(i.getReferenceItem().getType(), material.getMaterialType().getTypeName()))
                                                            .filter(i -> {
                                                                var linking = i.getReferenceItem().getLinking();
                                                                return linking == null || Objects.equals(linking, material.getMaterialPower()) || Objects.equals(linking, material.getMaterialLength());
                                                            })
                                                            .findFirst();
                                                    if(contractCable.isEmpty()) {
                                                        throw new RuntimeException("Cabo do Contrato não encontrado");
                                                    }
                                                    newCable.setMaterial(rm);
                                                    newCable.setContractItem(contractCable.get());
                                                    if (material.getMaterialLength().startsWith("1"))
                                                        newCable.addItemQuantity(item.getMaterialQuantity() * 2.5);
                                                    if (material.getMaterialLength().startsWith("2"))
                                                        newCable.addItemQuantity(item.getMaterialQuantity() * 8.5);
                                                    if (material.getMaterialLength().startsWith("3"))
                                                        newCable.addItemQuantity(item.getMaterialQuantity() * 12.5);
                                                    preMeasurementStreet.addItem(newCable);
                                                    preMeasurementStreetItemRepository.save(newCable);
                                                }
                                        );
                            });
                });
            });
        });

        return ResponseEntity.ok().body(new DefaultResponse("Pré-Medição salva com sucesso"));
    }


    public ResponseEntity<?> getAll(String status) {
        AtomicInteger number = new AtomicInteger(1);

        List<PreMeasurementResponseDTO> measurements = preMeasurementRepository
                .findAllByStatusOrderByCreatedAtAsc(status)
                .stream()
                .sorted(Comparator.comparing(PreMeasurement::getPreMeasurementId))
                .map(p -> new PreMeasurementResponseDTO(
                        p.getPreMeasurementId(),
                        p.getContract().getContractId(),
                        p.getCity(),
                        p.getCreatedBy() != null ? p.getCreatedBy().getCompletedName() : "Desconhecido",
                        util.normalizeDate(p.getCreatedAt()),
                        "",
                        p.getTypePreMeasurement(),
                        Objects.equals(p.getTypePreMeasurement(), ContractType.INSTALLATION) ?
                                "badge-primary" : "badge-neutral",
                        p.getTypePreMeasurement(),
                        p.getTotalPrice() != null ? p.getTotalPrice().toString() : "0,00",
                        p.getStreets().stream()
                                .sorted(Comparator.comparing(PreMeasurementStreet::getPreMeasurementStreetId))
                                .map(s -> new PreMeasurementStreetResponseDTO(
                                        number.getAndIncrement(),
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
                                                        i.getContractItem().getContractItemId(),
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
                .findByPreMeasurementIdAndStatus(preMeasurementId, ContractStatus.PENDING);


        AtomicInteger number = new AtomicInteger(1);

        var preMeasurement = new PreMeasurementResponseDTO(
                p.getPreMeasurementId(),
                p.getContract().getContractId(),
                p.getCity(),
                p.getCreatedBy() != null ? p.getCreatedBy().getCompletedName() : "Desconhecido",
                util.normalizeDate(p.getCreatedAt()),
                "",
                p.getTypePreMeasurement(),
                Objects.equals(p.getTypePreMeasurement(), ContractType.INSTALLATION) ?
                        "badge-primary" : "badge-neutral",
                p.getTypePreMeasurement(),
                p.getTotalPrice() != null ? p.getTotalPrice().toString() : "0,00",
                p.getStreets().stream()
                        .sorted(Comparator.comparing(PreMeasurementStreet::getPreMeasurementStreetId))
                        .map(s -> new PreMeasurementStreetResponseDTO(
                                number.getAndIncrement(),
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
                                                i.getContractItem().getContractItemId(),
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
