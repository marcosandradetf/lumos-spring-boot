package com.lumos.lumosspring.pre_measurement.service;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative;
import com.lumos.lumosspring.contract.repository.ContractRepository;
import com.lumos.lumosspring.pre_measurement.dto.*;
import com.lumos.lumosspring.pre_measurement.dto.response.PreMeasurementStreetItemResponseDTO;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    @Caching(evict = {
            @CacheEvict(cacheNames = "getPreMeasurements", allEntries = true),
            @CacheEvict(cacheNames = "getPreMeasurementById", allEntries = true)
    })
    public boolean setStatus(Long preMeasurementId) {
        var preMeasurement = preMeasurementRepository.findById(preMeasurementId);

        if (preMeasurement.isEmpty()) {
            return false;
        }

        switch (preMeasurement.get().getStatus()) {
            case (ContractStatus.PENDING):
                preMeasurement.get().setStatus(ContractStatus.WAITING_CONTRACTOR);
                break;
            case (ContractStatus.WAITING_CONTRACTOR):
                preMeasurement.get().setStatus(ContractStatus.AVAILABLE);
                break;
//            case (ContractStatus.VALIDATING):
//                preMeasurement.get().setStatus(ContractStatus.AVAILABLE);
//                break;
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

    /**
     * TODO
     * SALVAMENTO DA PRÉ-MEDIÇÃO
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "getPreMeasurements", allEntries = true),
            @CacheEvict(cacheNames = "getPreMeasurementById", allEntries = true),
            @CacheEvict(cacheNames = "GetContractsForPreMeasurement", allEntries = true)

    })
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

        streets.forEach(streetDTO -> {
            PreMeasurementStreet preMeasurementStreet = new PreMeasurementStreet();
            var street = streetDTO.getStreet();

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
            streetDTO.getItems().forEach(itemDTO -> {
                materialRepository.findByIdWithGraphType(itemDTO.getMaterialId()).ifPresent(material -> {
                    var contractItem = searchContractItem(contract, material);
                    if (contractItem.isEmpty()) {
                        throw new RuntimeException("Item do Contrato não encontrado");
                    }

                    var newItem = new PreMeasurementStreetItem();
                    newItem.setPreMeasurementStreet(preMeasurementStreet);
                    newItem.setPreMeasurement(preMeasurement);
                    newItem.setMaterial(material);
                    newItem.setItemStatus(ItemStatus.PENDING);
                    newItem.setItemQuantity(itemDTO.getMaterialQuantity());
                    newItem.setContractItem(contractItem.get());
                    preMeasurementStreetItemRepository.save(newItem);

                    insertServices(material.getMaterialType().getTypeName(), contract, newItem);

                    insertDependencyItems(material, itemDTO, preMeasurementStreet, contract);

                });
            });
        });


        var allItems = preMeasurementStreetItemRepository.findAllByPreMeasurement(preMeasurement);

        BigDecimal itemsPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal servicesPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        preMeasurement.setTotalPrice(itemsPrices.add(servicesPrices));
        preMeasurementRepository.save(preMeasurement);

        return ResponseEntity.ok().body(new DefaultResponse("Pré-Medição salva com sucesso"));
    }

    /**
     * TODO
     * METODO PARA ADICIONAR SERVIÇOS RELACIONADOS NA PRÉ-MEDIÇÃO
     * EXEMPLO: PRIMEIRA CHAMADA LED DE 100W / BRAÇO DE 1,5
     * EXEMPLO: SEGUNDA CHAMADA LED DE 120W / BRAÇO DE 2,5
     */
    @Transactional
    protected void insertServices(String actualItemType, Contract contract, PreMeasurementStreetItem actualItem) {
        if (!List.of("LED", "BRAÇO").contains(actualItemType.toUpperCase())) return;

        contract.getContractItemsQuantitative().stream()
                .filter(i -> "SERVIÇO".equalsIgnoreCase(i.getReferenceItem().getType()))
                .filter(i -> actualItemType.equalsIgnoreCase(i.getReferenceItem().getItemDependency()))
                .forEach(contractService -> {
                    actualItem.setContractServiceIdMask(contractService.getContractItemId());
                    actualItem.setContractServiceDividerPrices(
                            contractService.getUnitPrice().multiply(BigDecimal.valueOf(actualItem.getItemQuantity()))
                    );
                });
    }

    /**
     * ME TODO PARA ADICIONAR MATERIAIS DEPENDENTES NA PRÉ-MEDIÇÃO
     **/
    @Transactional
    protected void insertDependencyItems(Material material, PreMeasurementStreetItemDTO itemDTO, PreMeasurementStreet preMeasurementStreet, Contract contract) {
        material.getRelatedMaterials().stream()
                .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("REL"))
                .findFirst().ifPresent(rm -> {
                    insertOrUpdateRelay(preMeasurementStreet, rm, itemDTO.getMaterialQuantity(), contract);
                });

        material.getRelatedMaterials().stream()
                .filter(m -> util.normalizeWord(m.getMaterialType().getTypeName()).startsWith("CAB"))
                .findFirst().ifPresent(rm -> {
                    insertOrUpdateCable(preMeasurementStreet, rm, material.getMaterialLength(), itemDTO.getMaterialQuantity(), contract);
                });
    }

    @Transactional
    protected void insertOrUpdateRelay(PreMeasurementStreet preMeasurementStreet, Material rm, int quantity, Contract contract) {
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
            newRelay.setContractItem(contractRelay.get());
            preMeasurementStreetItemRepository.save(newRelay);
        }
    }

    @Transactional
    protected void insertOrUpdateCable(PreMeasurementStreet preMeasurementStreet, Material cable, String armLength, int quantity, Contract contract) {
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
            newCable.setContractItem(contractCable.get());
            preMeasurementStreetItemRepository.save(newCable);
        }
    }

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

    @Cacheable("getPreMeasurements")
    public ResponseEntity<?> getAll(String status) {
        List<PreMeasurementResponseDTO> measurements = preMeasurementRepository
                .findAllByStatusOrderByCreatedAtAsc(status)
                .stream()
                .sorted(Comparator.comparing(PreMeasurement::getPreMeasurementId))
                .map(this::convertToPreMeasurementResponseDTO).toList();

        return ResponseEntity.ok().body(measurements);
    }

    @Cacheable("getPreMeasurementById")
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
                        .filter(s -> !Objects.equals(s.getStreetStatus(), ItemStatus.CANCELLED))
                        .sorted(Comparator.comparing(PreMeasurementStreet::getPreMeasurementStreetId))
                        .map(s -> new PreMeasurementStreetResponseDTO(
                                number.getAndIncrement(),
                                s.getPreMeasurementStreetId(),
                                s.getLastPower(),
                                s.getLatitude(),
                                s.getLongitude(),
                                s.getStreet(),
                                s.getNeighborhood(),
                                s.getCity(),
                                s.getStreetStatus(),
                                s.getItems() != null ? s.getItems().stream()
                                        .filter(i -> !Objects.equals(i.getItemStatus(), ItemStatus.CANCELLED))
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

    /**
     * MÉTODO PARA SALVAR AS MODIFICAÇOES NA PRÉ-MEDIÇÃO
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "getPreMeasurements", allEntries = true),
            @CacheEvict(cacheNames = "getPreMeasurementById", allEntries = true)
    })
    @Transactional
    public ResponseEntity<?> saveModifications(ModificationsDTO modificationsDTO) {
        var cancelledStreets = modificationsDTO.getCancelledStreets();
        var cancelledItems = modificationsDTO.getCancelledItems();
        var changedItems = modificationsDTO.getChangedItems();

        if (!cancelledStreets.isEmpty())
            cancelStreets(cancelledStreets);

        if (!cancelledItems.isEmpty())
            cancelItems(cancelledItems);

        if (!changedItems.isEmpty())
            changeItems(changedItems);


        return ResponseEntity.ok(new DefaultResponse("Itens Atualizados com Sucesso!"));
    }

    protected void cancelStreets(List<CancelledStreets> cancelledStreets) {
        List<Long> streetIds = cancelledStreets.stream()
                .filter(Objects::nonNull)
                .map(CancelledStreets::getStreetId)
                .toList();

        List<PreMeasurementStreet> allStreets = preMeasurementStreetRepository.findAllById(streetIds)
                .stream()
                .peek(s -> {
                    s.setStreetStatus(ItemStatus.CANCELLED);
                    if (s.getItems() != null) {
                        s.getItems().forEach(item -> item.setItemStatus(ItemStatus.CANCELLED));
                    }
                })
                .toList();

        Set<Long> preMeasurementIds = allStreets.stream()
                .map(s -> s.getPreMeasurement().getPreMeasurementId())
                .collect(Collectors.toSet());

        if (preMeasurementIds.size() > 1) {
            throw new IllegalStateException("Ruas de múltiplas pré-medições não podem ser canceladas juntas");
        }

        BigDecimal itemsPrices = allStreets.stream()
                .map(PreMeasurementStreet::getItems)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(PreMeasurementStreetItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal servicesPrices = allStreets.stream()
                .map(PreMeasurementStreet::getItems)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPrice = itemsPrices.add(servicesPrices);

        PreMeasurement preMeasurement = allStreets.getFirst().getPreMeasurement();
        preMeasurement.subtractTotalPrice(totalPrice);
        preMeasurement.setStatus(ContractStatus.AVAILABLE);
        preMeasurementRepository.save(preMeasurement);
    }

    protected void cancelItems(List<CancelledItems> cancelledItems) {
        List<Long> itemsIds = cancelledItems.stream()
                .filter(Objects::nonNull)
                .map(CancelledItems::getItemId)
                .toList();

        List<PreMeasurementStreetItem> allItems = preMeasurementStreetItemRepository.findAllById(itemsIds)
                .stream()
                .peek(s ->
                        s.setItemStatus(ItemStatus.CANCELLED)
                )
                .toList();

        Set<Long> preMeasurementIds = allItems.stream()
                .map(item -> item.getPreMeasurement().getPreMeasurementId())
                .collect(Collectors.toSet());

        if (preMeasurementIds.size() > 1) {
            throw new IllegalStateException("Itens de múltiplas pré-medições não podem ser cancelados juntos");
        }

        BigDecimal itemsPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal servicesPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal totalPrice = itemsPrices.add(servicesPrices);

        PreMeasurement preMeasurement = allItems.getFirst().getPreMeasurement();
        preMeasurement.subtractTotalPrice(totalPrice);
        preMeasurement.setStatus(ContractStatus.AVAILABLE);
        preMeasurementRepository.save(preMeasurement);
    }

    protected void changeItems(List<ChangedItems> changedItems) {
        List<Long> itemsIds = changedItems.stream()
                .filter(Objects::nonNull)
                .map(ChangedItems::getItemId)
                .toList();

        // Carrega os itens antigos com base nos IDs
        List<PreMeasurementStreetItem> oldItems = preMeasurementStreetItemRepository.findAllById(itemsIds)
                .stream()
                .toList();

        Set<Long> preMeasurementIds = oldItems.stream()
                .map(item -> item.getPreMeasurement().getPreMeasurementId())
                .collect(Collectors.toSet());

        if (preMeasurementIds.size() > 1) {
            throw new IllegalStateException("Itens de múltiplas pré-medições não podem ser cancelados juntos");
        }

        Map<Long, PreMeasurementStreetItem> itemMap = oldItems.stream()
                .collect(Collectors.toMap(PreMeasurementStreetItem::getPreMeasurementStreetItemId, i -> i));

        // Carrega os serviços do contrato relacionado
        // Pega o contrato a partir de um dos itens (todos são da mesma pré-med)
        Optional<Contract> contractOpt = oldItems.stream()
                .map(PreMeasurementStreetItem::getPreMeasurementStreet)
                .filter(Objects::nonNull)
                .map(PreMeasurementStreet::getPreMeasurement)
                .filter(Objects::nonNull)
                .map(PreMeasurement::getContract)
                .filter(Objects::nonNull)
                .findFirst();

        List<ContractItemsQuantitative> services = contractOpt
                .map(contract -> getAllServices(contract.getContractId()))
                .orElse(Collections.emptyList());


        // Soma total de preço de serviços antigos
        BigDecimal oldServicesPrice = oldItems.stream()
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Soma total de preço de itens antigos
        BigDecimal oldItemsPrice = oldItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Atualiza os itens com as novas quantidades e preços
        var newItems = changedItems.stream()
                .map(cs -> Map.entry(itemMap.get(cs.getItemId()), cs.getQuantity()))
                .filter(entry -> entry.getKey() != null)
                .peek(entry -> {
                    var item = entry.getKey();
                    var quantity = entry.getValue();

                    item.setItemStatus(ItemStatus.APPROVED);
                    item.setItemQuantity(quantity);
                    item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));

                    if (item.getContractServiceIdMask() != null) {
                        var typeItem = item.getMaterial().getMaterialType().getTypeName();
                        item.setContractServiceDividerPrices(BigDecimal.ZERO, true);
                        services.stream()
                                .filter(serviceItem ->
                                        typeItem.equalsIgnoreCase(serviceItem.getReferenceItem().getItemDependency()))
                                .forEach(contractService ->
                                        item.setContractServiceDividerPrices(contractService.getUnitPrice().multiply(BigDecimal.valueOf(quantity))
                                ));
                    }

                    Optional.ofNullable(item.getPreMeasurementStreet())
                            .ifPresent(s -> s.setStreetStatus(ItemStatus.APPROVED));
                })
                .map(Map.Entry::getKey)
                .toList();

        // Soma total de preço de serviços novos
        BigDecimal newServicesPrice = newItems.stream()
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Soma total de preço de itens novos
        BigDecimal newItemsPrice = newItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (newItems.isEmpty()) {
            throw new IllegalStateException("Nenhum item foi alterado.");
        }

        PreMeasurement preMeasurement = newItems.getFirst().getPreMeasurement();
        preMeasurement.subtractTotalPrice(oldItemsPrice.add(oldServicesPrice));
        preMeasurement.sumTotalPrice(newItemsPrice.add(newServicesPrice));
        preMeasurement.setStatus(ContractStatus.AVAILABLE);
        preMeasurementRepository.save(preMeasurement);
    }

    private BigDecimal calculateServicesPrice(List<PreMeasurementStreetItem> items, List<ContractItemsQuantitative> services) {
        return items.stream()
                .filter(item -> item.getContractServiceIdMask() != null && !item.getContractServiceIdMask().isBlank())
                .flatMap(item -> {
                    List<Long> serviceIds = this.util.extractMaskToList(item.getContractServiceIdMask());
                    return serviceIds.stream().map(serviceId -> Map.entry(serviceId, item));
                })
                .map(entry -> {
                    Long serviceId = entry.getKey();
                    PreMeasurementStreetItem item = entry.getValue();

                    BigDecimal unitPrice = services.stream()
                            .filter(s -> Objects.equals(s.getContractItemId(), serviceId))
                            .findFirst()
                            .map(ContractItemsQuantitative::getUnitPrice)
                            .orElse(BigDecimal.ZERO);

                    var quantity = item.getItemQuantity() != null ? item.getItemQuantity() : 0;
                    return unitPrice.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ContractItemsQuantitative> getAllServices(Long contractId) {
        return contractRepository.findContractByContractId(contractId)
                .map(contract -> contract.getContractItemsQuantitative().stream()
                        .filter(ciq -> {
                            var type = ciq.getReferenceItem().getType();
                            return type != null && type.equalsIgnoreCase("SERVIÇO");
                        })
                        .toList()
                )
                .orElse(Collections.emptyList());
    }

}
