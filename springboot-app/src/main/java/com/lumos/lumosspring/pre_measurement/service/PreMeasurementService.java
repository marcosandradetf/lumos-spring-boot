package com.lumos.lumosspring.pre_measurement.service;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative;
import com.lumos.lumosspring.contract.entities.ContractReferenceItem;
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
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
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
    public ResponseEntity<?> savePreMeasurement(PreMeasurementDTO preMeasurementDTO, String userUUID) {
        if (userUUID == null || userUUID.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("user UUID is required"));
        }

        var contract = contractRepository.findContractByContractId(preMeasurementDTO.getContractId()).orElse(null);
        if (contract == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Contract not found"));
        }

        var user = userRepository.findByIdUser(UUID.fromString(userUUID));
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("User not found"));
        }

//        if (!contract.getStatus().equals(ContractStatus.PENDING)) {
//            notificationService.sendNotificationForRole(
//                    "Pré-medição de ".concat(Objects.requireNonNull(contract.getContractor())),
//                    "Sua pré-medição não foi salva, pois já foi enviada anteriormente pelo usuário ".concat(user.get().getName()).concat(" em caso de dúvidas, procure sua empresa para evitar inconsistências na pré-medição."),
//                    Routes.PRE_MEASUREMENT_PROGRESS.concat("/").concat(String.valueOf(contract.getContractId())),
//                    Role.Values.RESPONSAVEL_TECNICO,
//                    util.getDateTime(),
//                    NotificationType.WARNING
//            );
//            return ResponseEntity.ok().body(new ErrorResponse("Já foi enviada uma pré-medição anterior para este contrato."));
//        }

        contract.setStatus(ContractStatus.VALIDATING);
        contractRepository.save(contract);

        var preMeasurement = preMeasurementRepository.findByContract_ContractId(contract.getContractId())
                .orElseGet(() -> {
                    PreMeasurement newPre = new PreMeasurement();
                    newPre.setContract(contract);
                    newPre.setCreatedBy(user.orElse(null));
                    newPre.setCreatedAt(util.getDateTime());
                    newPre.setTypePreMeasurement(ContractType.INSTALLATION);
                    newPre.setStatus(ContractStatus.PENDING);
                    newPre.setCity(contract.getContractor());
                    return preMeasurementRepository.save(newPre);
                });

        var streets = preMeasurementDTO.getStreets();

        for (var streetDTO : streets) {
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
            for (var itemDTO : streetDTO.getItems()) {
                for (var contractItem : contract.getContractItemsQuantitative()) {
                    if (contractItem.getContractItemId() == itemDTO.getItemContractId()) {
                        var newItem = new PreMeasurementStreetItem();
                        newItem.setPreMeasurementStreet(preMeasurementStreet);
                        newItem.setPreMeasurement(preMeasurement);
//                    newItem.setMaterial(material);
                        newItem.setItemStatus(ItemStatus.PENDING);
                        newItem.setMeasuredItemQuantity(itemDTO.getMaterialQuantity());
                        newItem.setContractItem(contractItem);
                        preMeasurementStreetItemRepository.save(newItem);

                        var referenceItem = contractItem.getReferenceItem();
                        if (referenceItem.getType() != null) {
                            insertServices(referenceItem.getType(), contract, newItem);
                            insertProject(referenceItem.getType(), contract, newItem);

                            // exemplo braco de 1,5
                            insertDependencyItems(
                                    referenceItem, //braço
                                    itemDTO,
                                    preMeasurementStreet,
                                    contract
                            );
                        }
                    }
                }
            }
        }


        var allItems = preMeasurementStreetItemRepository.findAllByPreMeasurement(preMeasurement);

        BigDecimal itemsPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal servicesPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        preMeasurement.setTotalPrice(itemsPrices.add(servicesPrices));
        preMeasurementRepository.save(preMeasurement);

        return ResponseEntity.ok().body(new DefaultResponse(preMeasurement.getPreMeasurementId().toString()));
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

        for (var iq : contract.getContractItemsQuantitative()) {
            var service = iq.getReferenceItem();
            if (Objects.requireNonNull(service.getType()).equalsIgnoreCase( "SERVIÇO")) {
                if(Objects.requireNonNull(service.getItemDependency()).equalsIgnoreCase(actualItemType)) {
                    actualItem.setContractServiceIdMask(iq.getContractItemId());
                    actualItem.setContractServiceDividerPrices(
                            iq.getUnitPrice().multiply(BigDecimal.valueOf(actualItem.getMeasuredItemQuantity()))
                    );
                }
            }
        }
    }

    @Transactional
    protected void insertProject(String actualItemType, Contract contract, PreMeasurementStreetItem actualItem) {
        if (!"LED".equalsIgnoreCase(actualItemType)) return;

        for (var iq : contract.getContractItemsQuantitative()) {
            var project = iq.getReferenceItem();
            if (Objects.requireNonNull(project.getType()).equalsIgnoreCase( "PROJETO")) {
                if(Objects.requireNonNull(project.getItemDependency()).equalsIgnoreCase(actualItemType)) {
                    actualItem.setContractServiceIdMask(iq.getContractItemId());
                    actualItem.setContractServiceDividerPrices(
                            iq.getUnitPrice().multiply(BigDecimal.valueOf(actualItem.getMeasuredItemQuantity()))
                    );
                }
            }
        }
    }

    /**
     * ME TODO PARA ADICIONAR MATERIAIS DEPENDENTES NA PRÉ-MEDIÇÃO
     **/
    @Transactional
    protected void insertDependencyItems(ContractReferenceItem referenceItem, PreMeasurementStreetItemDTO itemDTO, PreMeasurementStreet preMeasurementStreet, Contract contract) {
        for (var iq : contract.getContractItemsQuantitative()) {
            var reference = iq.getReferenceItem();
            if (reference.getItemDependency() != null && Objects.equals(reference.getItemDependency(), referenceItem.getType())) {
                insertOrUpdateItem(
                        referenceItem, //braço
                        preMeasurementStreet,
                        iq, // CABO FLEXÍVEL 1,5MM
                        itemDTO.getMaterialQuantity()
                );
            }
        }
    }

    @Transactional
    protected void insertOrUpdateItem(ContractReferenceItem referenceItem,
                                      PreMeasurementStreet preMeasurementStreet,
                                      ContractItemsQuantitative itemInsert,
                                      int quantity) {
        //ex.: itemInsert = CABO FLEXÍVEL 1,5MM
        //ex.: referenceItem = BRAÇO
        var referenceInsert = itemInsert.getReferenceItem();
        var existingItem = preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet)
                .stream()
                .filter(i -> i.getContractItem().equals(itemInsert))
                .findFirst();

        if (Objects.equals(referenceInsert.getType(), "RELÉ")) {
            if (existingItem.isPresent()) {
                existingItem.get().addItemQuantity(quantity, true);
                preMeasurementStreetItemRepository.save(existingItem.get());
            } else {
                var newRelay = new PreMeasurementStreetItem();
                newRelay.addItemQuantity(quantity);
                preMeasurementStreet.addItem(newRelay);
                newRelay.setContractItem(itemInsert);
                preMeasurementStreetItemRepository.save(newRelay);
            }
        } else if (Objects.equals(referenceInsert.getType(), "CABO"))
            if (existingItem.isPresent()) {
                double multiplier = getCableMultiplier(Objects.requireNonNull(referenceItem.getLinking()));
                existingItem.get().addItemQuantity(quantity * multiplier, true);
                preMeasurementStreetItemRepository.save(existingItem.get());
            } else {
                var newCable = new PreMeasurementStreetItem();
                preMeasurementStreet.addItem(newCable);

                double multiplier = getCableMultiplier(Objects.requireNonNull(referenceItem.getLinking()));
                newCable.addItemQuantity(quantity * multiplier);
                newCable.setContractItem(itemInsert);
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
                            (linking.equalsIgnoreCase(material.getIdMaterial().toString())) ||
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
                                                i.getMeasuredItemQuantity(),
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
                    item.setMeasuredItemQuantity(quantity);
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

                    var quantity = item.getMeasuredItemQuantity() != null ? item.getMeasuredItemQuantity() : 0;
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

    @Transactional
    public ResponseEntity<?> importPreMeasurements(PreMeasurementDTO preMeasurement, String userUUID) {
        return this.savePreMeasurement(preMeasurement, userUUID);
    }

    @Transactional
    public ResponseEntity<?> deletePreMeasurementStreets(DeletePreMeasurementDTO deleteDTO) {
        this.preMeasurementStreetItemRepository.deleteByStreet(deleteDTO.getPreMeasurementStreetIds());
        this.preMeasurementStreetRepository.deleteByStreet(deleteDTO.getPreMeasurementStreetIds());

        var preMeasurement = this.preMeasurementRepository.findByPreMeasurementId(deleteDTO.getPreMeasurementId());
        if (preMeasurement == null) {
            return ResponseEntity.notFound().build();
        }
        updatePremeasurementPrice(preMeasurement);

        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> deleteProject(DeletePreMeasurementDTO deleteDTO) {

        var streets = preMeasurementStreetRepository.findByIds(deleteDTO.getPreMeasurementStreetIds());
        if (streets.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        for (PreMeasurementStreet street : streets) {
            for (PreMeasurementStreetItem item : street.getItems()) {
                if (item.getContractServiceIdMask() != null) {
                    item.clearContractServices();
                    insertServices(
                            item.getMaterial().getMaterialType().getTypeName(),
                            street.getPreMeasurement().getContract(),
                            item
                    );
                }
            }
        }
        preMeasurementStreetRepository.saveAll(streets);


        return ResponseEntity.ok().build();
    }

    private void updatePremeasurementPrice(PreMeasurement preMeasurement) {
        var allItems = preMeasurementStreetItemRepository.findAllByPreMeasurement(preMeasurement);

        BigDecimal itemsPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal servicesPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        preMeasurement.setTotalPrice(itemsPrices.add(servicesPrices));
        preMeasurementRepository.save(preMeasurement);
    }

}
