package com.lumos.lumosspring.pre_measurement.service;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative;
import com.lumos.lumosspring.contract.repository.ContractRepository;
import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementStreetItemResponseDTO;
import com.lumos.lumosspring.pre_measurement.dto.PreMeasurementDTO;
import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementResponseDTO;
import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementStreetResponseDTO;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementRepository;
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetItemRepository;
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository;
import com.lumos.lumosspring.notification.service.NotificationService;
import com.lumos.lumosspring.notification.service.Routes;
import com.lumos.lumosspring.stock.entities.Material;
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

    public boolean setStatus(Long preMeasurementId) {
        var preMeasurement = preMeasurementRepository.findById(preMeasurementId);

        if (preMeasurement.isEmpty()) {
            return false;
        }

        switch (preMeasurement.get().getStatus()) {
            case (ContractStatus.PENDING):
                preMeasurement.get().setStatus(ContractStatus.WAITING);
                break;
            case (ContractStatus.WAITING):
                preMeasurement.get().setStatus(ContractStatus.VALIDATING);
                break;
            case (ContractStatus.VALIDATING):
                preMeasurement.get().setStatus(ContractStatus.AVAILABLE);
                break;
            case (ContractStatus.AVAILABLE):
                preMeasurement.get().setStatus(ContractStatus.IN_PROGRESS);
                break;
            case (ContractStatus.IN_PROGRESS):
                preMeasurement.get().setStatus(ContractStatus.FINISHED);
                break;
        }

        preMeasurementRepository.save(preMeasurement.get());
        return true;
    }

    //    analisar se duplicata ocorre novamente
//    corrigir soma de valores da pre-medicao
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
            return ResponseEntity.ok().body(new ErrorResponse("Já foi enviada uma pré-medição anterior para este contrato."));
        }

        contract.setStatus(ContractStatus.VALIDATING);
        contractRepository.save(contract);
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
                    var contractItem = searchContractItem(contract, material);
                    if (contractItem.isEmpty()) {
                        throw new RuntimeException("Item do Contrato não encontrado");
                    }

                    var newItem = new PreMeasurementStreetItem();
                    newItem.setPreMeasurementStreet(preMeasurementStreet);
                    newItem.setMaterial(material);
                    newItem.setItemStatus(ItemStatus.PENDING);
                    newItem.setItemQuantity(item.getMaterialQuantity());
                    preMeasurementStreetItemRepository.save(newItem);
                    preMeasurementStreetItemRepository.flush();
                    newItem.setContractItem(contractItem.get());
                    //    analisar se duplicata ocorre novamente
//    corrigir soma de valores da pre-medicao
                    preMeasurementStreetItemRepository.save(newItem); // Salvar a relação
                    if (Objects.equals(material.getMaterialType().getTypeName(), "LED")) {
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

                    material.getRelatedMaterials().stream()
                            .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("REL"))
                            .findFirst().ifPresent(rm -> {
                                insertOrUpdateRelay(preMeasurementStreet, rm, item.getMaterialQuantity(), contract);
                            });

                    material.getRelatedMaterials().stream()
                            .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("CAB"))
                            .findFirst().ifPresent(rm -> {
                                insertOrUpdateCable(preMeasurementStreet, rm, material.getMaterialLength(), item.getMaterialQuantity(), contract);
                            });
                });
            });
        });

        return ResponseEntity.ok().body(new DefaultResponse("Pré-Medição salva com sucesso"));
    }

    @Transactional
    public void insertOrUpdateRelay(PreMeasurementStreet preMeasurementStreet, Material rm, int quantity, Contract contract) {
        var existingRelay = preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet)
                .stream()
                .filter(i -> i.getMaterial().getIdMaterial().equals(rm.getIdMaterial()))
                .findFirst();

        if (existingRelay.isPresent()) {
            existingRelay.get().addItemQuantity(quantity, true);
            preMeasurementStreetItemRepository.save(existingRelay.get());
        } else {
            var contractRelay = searchContractItem(contract, rm);
            if (contractRelay.isEmpty()) {
                throw new RuntimeException("Relé do Contrato não encontrado");
            }
            var newRelay = new PreMeasurementStreetItem();
            newRelay.setMaterial(rm);
            newRelay.addItemQuantity(quantity);
            preMeasurementStreet.addItem(newRelay);
            preMeasurementStreetItemRepository.save(newRelay);

            // Agora que o objeto foi salvo, podemos definir a relação e salvar novamente
            newRelay.setContractItem(contractRelay.get());
            preMeasurementStreetItemRepository.save(newRelay);
        }
    }

    @Transactional
    public void insertOrUpdateCable(PreMeasurementStreet preMeasurementStreet, Material cable, String armLength, int quantity, Contract contract) {
        var existingCable = preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet)
                .stream()
                .filter(i -> i.getMaterial().getIdMaterial().equals(cable.getIdMaterial()))
                .findFirst();

        if (existingCable.isPresent()) {
            double multiplier = getCableMultiplier(armLength);
            existingCable.get().addItemQuantity(quantity * multiplier, true);
            preMeasurementStreetItemRepository.save(existingCable.get());
        } else {
            var contractCable = searchContractItem(contract, cable);
            if (contractCable.isEmpty()) {
                throw new RuntimeException("Cabo do Contrato não encontrado");
            }
            var newCable = new PreMeasurementStreetItem();
            newCable.setMaterial(cable);
            preMeasurementStreet.addItem(newCable);

            double multiplier = getCableMultiplier(armLength);
            newCable.addItemQuantity(quantity * multiplier);
            preMeasurementStreetItemRepository.save(newCable);

            // Definir a relação e salvar novamente
            newCable.setContractItem(contractCable.get());
            preMeasurementStreetItemRepository.save(newCable);
        }
    }

    /**
     * Método auxiliar para calcular o multiplicador do cabo.
     */
    private double getCableMultiplier(String armLength) {
        return switch (armLength.charAt(0)) {
            case '1' -> 2.5;
            case '2' -> 8.5;
            case '3' -> 12.5;
            default -> throw new IllegalArgumentException("Comprimento do braço inválido: " + armLength);
        };
    }

    private Optional<ContractItemsQuantitative> searchContractItem(Contract contract, Material material) {
        return contract.getContractItemsQuantitative().stream()
                .filter(i -> {
                    var referenceItem = i.getReferenceItem();

                    var type = referenceItem.getType();
                    var materialType = material.getMaterialType();
                    if (type == null || materialType == null || materialType.getTypeName() == null) return false;

                    return type.equalsIgnoreCase(materialType.getTypeName());
                })
                .filter(i -> {
                    var referenceItem = i.getReferenceItem();

                    var linking = referenceItem.getLinking();
                    var materialPower = material.getMaterialPower();
                    var materialLength = material.getMaterialLength();

                    // Se linking for nulo, não filtra (ou ajuste confocablee a lógica esperada)
                    return linking == null ||
                            (linking.equalsIgnoreCase(materialPower)) ||
                            (linking.equalsIgnoreCase(materialLength));
                }).findFirst();
    }

    public ResponseEntity<?> getAll(String status) {
        List<PreMeasurementResponseDTO> measurements = preMeasurementRepository
                .findAllByStatusOrderByCreatedAtAsc(status)
                .stream()
                .sorted(Comparator.comparing(PreMeasurement::getPreMeasurementId))
                .map(this::convertToPreMeasurementResponseDTO).toList();

        return ResponseEntity.ok().body(measurements);
    }

    public ResponseEntity<?> getPreMeasurement(long preMeasurementId) {
        PreMeasurement p = preMeasurementRepository
                .findByPreMeasurementId(preMeasurementId);

        return ResponseEntity.ok().body(convertToPreMeasurementResponseDTO(p));
    }

    public PreMeasurementResponseDTO convertToPreMeasurementResponseDTO(PreMeasurement p) {
        AtomicInteger number = new AtomicInteger(1);
        return new PreMeasurementResponseDTO(
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
                p.getStatus(),
                p.getStreets().stream()
                        .sorted(Comparator.comparing(PreMeasurementStreet::getPreMeasurementStreetId))
                        .map(s -> new PreMeasurementStreetResponseDTO(
                                number.getAndIncrement(),
                                s.getPreMeasurementStreetId(),
                                s.getLastPower(),
                                s.getLatitude(),
                                s.getLongitude(),
                                s.getStreet(),
                                s.getStreetStatus(),
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
                                                i.getItemQuantity(),
                                                i.getItemStatus()
                                        )).toList()
                                        : List.of() // Retorna lista vazia se `s.getItems()` for null
                        )).toList()
        );
    }
}
