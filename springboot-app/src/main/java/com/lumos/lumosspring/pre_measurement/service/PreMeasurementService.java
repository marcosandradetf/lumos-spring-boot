package com.lumos.lumosspring.pre_measurement.service;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative;
import com.lumos.lumosspring.contract.entities.ContractReferenceItem;
import com.lumos.lumosspring.contract.repository.ContractRepository;
import com.lumos.lumosspring.fileserver.service.MinioService;
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
import com.lumos.lumosspring.notifications.service.NotificationService;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.*;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PreMeasurementService {
    private final PreMeasurementStreetRepository preMeasurementStreetRepository;
    private final PreMeasurementRepository preMeasurementRepository;
    private final PreMeasurementStreetItemRepository preMeasurementStreetItemRepository;
    private final UserRepository userRepository;
    private final Util util;
    private final ContractRepository contractRepository;
    private final NotificationService notificationService;
    private final MinioService minioService;
    private final JdbcTemplate jdbcTemplate;

    public PreMeasurementService(PreMeasurementStreetRepository preMeasurementStreetRepository,
                                 MaterialRepository materialRepository,
                                 PreMeasurementRepository preMeasurementRepository,
                                 PreMeasurementStreetItemRepository preMeasurementStreetItemRepository,
                                 UserRepository userRepository, Util util,
                                 ContractRepository contractRepository,
                                 NotificationService notificationService,
                                 MinioService minioService, JdbcTemplate jdbcTemplate) {
        this.preMeasurementStreetRepository = preMeasurementStreetRepository;
        this.preMeasurementRepository = preMeasurementRepository;
        this.preMeasurementStreetItemRepository = preMeasurementStreetItemRepository;
        this.userRepository = userRepository;
        this.util = util;
        this.contractRepository = contractRepository;
        this.notificationService = notificationService;
        this.minioService = minioService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getPreMeasurements", allEntries = true),
            @CacheEvict(cacheNames = "getPreMeasurementById", allEntries = true)
    })
    public boolean setStatus(Long preMeasurementId, Integer step) {
        var preMeasurement = preMeasurementRepository.findById(preMeasurementId);

        if (preMeasurement.isEmpty()) {
            return false;
        }

        boolean updated = false;
        for (var street : preMeasurement.get().getStreets()) {
            if (street.getStep().equals(step)) {
                var status = switch (street.getStreetStatus()) {
                    case (ContractStatus.PENDING) -> ContractStatus.WAITING_CONTRACTOR;
                    case (ContractStatus.WAITING_CONTRACTOR) -> ContractStatus.AVAILABLE;
//            case (ContractStatus.VALIDATING):
//                preMeasurement.get().setStatus(ContractStatus.AVAILABLE);
//                break;
                    case (ContractStatus.AVAILABLE) -> ContractStatus.IN_PROGRESS;
                    case (ContractStatus.IN_PROGRESS) -> ContractStatus.FINISHED;
                    default -> null;
                };
                if (status == null) return false;
                street.setStreetStatus(status);
                updated = true;
            }
        }

        if (updated) {
            preMeasurementRepository.save(preMeasurement.get());
        }

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
                    newPre.setTypePreMeasurement(ContractType.INSTALLATION);
                    newPre.setStatus(ContractStatus.PENDING);
                    newPre.setCity(contract.getContractor());
                    return preMeasurementRepository.save(newPre);
                });

        var step = preMeasurement.getSteps();

        var streets = preMeasurementDTO.getStreets();

        for (var streetDTO : streets) {
            PreMeasurementStreet preMeasurementStreet = new PreMeasurementStreet();
            var street = streetDTO.getStreet();

            var exists = preMeasurementStreetRepository.existsByDeviceIdAndDeviceStreetId(street.getDeviceId(), street.getPreMeasurementStreetId());
            if (exists) {
                continue;
            }

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
            preMeasurementStreet.setStep(step + 1);
            preMeasurementStreet.setCreatedBy(user.orElse(null));
            preMeasurementStreet.setCreatedAt(util.getDateTime());
            preMeasurementStreet.setDeviceStreetId(street.getPreMeasurementStreetId());
            preMeasurementStreet.setDeviceId(street.getDeviceId());

            preMeasurementStreetRepository.save(preMeasurementStreet);
            for (var itemDTO : streetDTO.getItems()) {
                for (var contractItem : contract.getContractItemsQuantitative()) {
                    if (contractItem.getContractItemId() == itemDTO.getItemContractId()) {
                        var continueLoop = false;
                        for (var type : List.of("SERVIÇO", "PROJETO", "CABO", "RELÉ")) {
                            if (contractItem.getReferenceItem().getType().equalsIgnoreCase(type))
                                continueLoop = true;
                        }
                        if (continueLoop) continue;

                        var newItem = new PreMeasurementStreetItem();
                        newItem.setPreMeasurementStreet(preMeasurementStreet);
                        newItem.setPreMeasurement(preMeasurement);
                        newItem.setItemStatus(ItemStatus.PENDING);
                        newItem.setMeasuredItemQuantity(itemDTO.getItemContractQuantity());
                        newItem.setContractItem(contractItem);
                        preMeasurementStreetItemRepository.save(newItem);

                        insertServices(contract, newItem, preMeasurementStreet);
                        insertProject(contract, newItem, preMeasurementStreet);

                        // exemplo braco de 1,5
                        insertDependencyItems(
                                newItem,
                                preMeasurementStreet,
                                contract
                        );
                    }
                }
            }
        }

        preMeasurement.newStep(); // <-- incrementa se já existia
        preMeasurementRepository.save(preMeasurement);

        var allItems = preMeasurementStreetItemRepository.findAllByPreMeasurement(preMeasurement);

        BigDecimal itemsPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

//        BigDecimal servicesPrices = allItems.stream()
//                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);

        preMeasurement.setTotalPrice(itemsPrices);
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
    protected void insertServices(Contract contract, PreMeasurementStreetItem currentStreetItem, PreMeasurementStreet preMeasurementStreet) {
        var currentContractItem = currentStreetItem.getContractItem().getReferenceItem();
        var currentContractItemType = currentContractItem.getType().toUpperCase();

        for (var service : contract.getContractItemsQuantitative()) {
            var serviceType = service.getReferenceItem().getType();
            if (serviceType.equalsIgnoreCase("SERVIÇO")) {
                var serviceItemDependency = service.getReferenceItem().getItemDependency();
                if (serviceItemDependency != null && serviceItemDependency.equalsIgnoreCase(currentContractItemType)) {
                    var quantity = currentStreetItem.getMeasuredItemQuantity();
                    var existingService = preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet)
                            .stream()
                            .filter(i -> i.getContractItem().equals(service))
                            .findFirst();

                    if (existingService.isPresent()) {
                        existingService.get().addItemQuantity(quantity, true);
                        preMeasurementStreetItemRepository.save(existingService.get());
                    } else {
                        var newService = new PreMeasurementStreetItem();
                        newService.addItemQuantity(quantity);
                        preMeasurementStreet.addItem(newService);
                        newService.setContractItem(service);
                        preMeasurementStreetItemRepository.save(newService);
                    }
                }
            }
        }
    }

    @Transactional
    protected void insertProject(Contract contract, PreMeasurementStreetItem currentStreetItem, PreMeasurementStreet preMeasurementStreet) {
        var currentContractItem = currentStreetItem.getContractItem().getReferenceItem();
        var currentContractItemType = currentContractItem.getType().toUpperCase();

        for (var project : contract.getContractItemsQuantitative()) {
            var projectType = project.getReferenceItem().getType();
            if (projectType.equalsIgnoreCase("PROJETO")) {
                var projectItemDependency = project.getReferenceItem().getItemDependency();
                if (projectItemDependency != null && projectItemDependency.equalsIgnoreCase(currentContractItemType)) {
                    var quantity = currentStreetItem.getMeasuredItemQuantity();
                    var existingProject = preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet)
                            .stream()
                            .filter(i -> i.getContractItem().equals(project))
                            .findFirst();

                    if (existingProject.isPresent()) {
                        existingProject.get().addItemQuantity(quantity, true);
                        preMeasurementStreetItemRepository.save(existingProject.get());
                    } else {
                        var newProject = new PreMeasurementStreetItem();
                        newProject.addItemQuantity(quantity);
                        preMeasurementStreet.addItem(newProject);
                        newProject.setContractItem(project);
                        preMeasurementStreetItemRepository.save(newProject);
                    }
                }
            }
        }
    }

    /**
     * ME TODO PARA ADICIONAR MATERIAIS DEPENDENTES NA PRÉ-MEDIÇÃO
     **/
    @Transactional
    protected void insertDependencyItems(PreMeasurementStreetItem currentStreetItem, PreMeasurementStreet preMeasurementStreet, Contract contract) {
        var currentContractItem = currentStreetItem.getContractItem().getReferenceItem();
        var currentContractItemType = currentContractItem.getType().toUpperCase();

        for (var contractItem : contract.getContractItemsQuantitative()) {
            var reference = contractItem.getReferenceItem();
            if (reference.getItemDependency() != null && reference.getItemDependency().equalsIgnoreCase(currentContractItemType)) {
                insertOrUpdateItem(
                        currentContractItem, //braço
                        preMeasurementStreet,
                        contractItem, // CABO FLEXÍVEL 1,5MM
                        currentStreetItem.getMeasuredItemQuantity()
                );
            }
        }
    }

    @Transactional
    protected void insertOrUpdateItem(ContractReferenceItem currentContractItem,
                                      PreMeasurementStreet preMeasurementStreet,
                                      ContractItemsQuantitative newContractItem,
                                      Double quantity) {
        //ex.: itemInsert = CABO FLEXÍVEL 1,5MM
        //ex.: referenceItem = BRAÇO
        var referenceInsert = newContractItem.getReferenceItem();
        var existingItem = preMeasurementStreetItemRepository.findAllByPreMeasurementStreet(preMeasurementStreet)
                .stream()
                .filter(i -> i.getContractItem().equals(newContractItem))
                .findFirst();

        if (referenceInsert.getType().equalsIgnoreCase("RELÉ")) {
            if (existingItem.isPresent()) {
                existingItem.get().addItemQuantity(quantity, true);
                preMeasurementStreetItemRepository.save(existingItem.get());
            } else {
                var newRelay = new PreMeasurementStreetItem();
                newRelay.addItemQuantity(quantity);
                preMeasurementStreet.addItem(newRelay);
                newRelay.setContractItem(newContractItem);
                preMeasurementStreetItemRepository.save(newRelay);
            }
        } else if (referenceInsert.getType().equalsIgnoreCase("CABO"))
            if (existingItem.isPresent() && currentContractItem.getLinking() != null) {
                double multiplier = getCableMultiplier(currentContractItem.getLinking());
                existingItem.get().addItemQuantity(quantity * multiplier, true);
                preMeasurementStreetItemRepository.save(existingItem.get());
            } else {
                var newCable = new PreMeasurementStreetItem();
                preMeasurementStreet.addItem(newCable);

                double multiplier = getCableMultiplier(Objects.requireNonNull(currentContractItem.getLinking()));
                newCable.addItemQuantity(quantity * multiplier);
                newCable.setContractItem(newContractItem);
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

//    @Cacheable("getPreMeasurements")
//    public ResponseEntity<?> getAll(String status) {
//        List<PreMeasurementResponseDTO> measurements = new ArrayList<>();
//
//        var streets = preMeasurementStreetRepository.findAllByStreetStatusOrderByCreatedAtAsc(status);
//        if (streets.isEmpty()) {
//            return ResponseEntity.ok().body(List.of());
//        }
//
//        var preMeasurementId = -1L;
//        for (var street : streets) {
//            if(street.getPreMeasurement().getPreMeasurementId() != preMeasurementId) {
//                preMeasurementId = street.getPreMeasurement().getPreMeasurementId();
//                measurements.add(convertToPreMeasurementResponseDTO(street.getPreMeasurement()));
//            }
//        }
//
//        return ResponseEntity.ok().body(measurements);
//
//    }

    @Cacheable("getPreMeasurements")
    public ResponseEntity<?> getAll(String status) {
        List<PreMeasurementStreet> streets = preMeasurementStreetRepository.getAllPreMeasurementsGroupByStep(status);

        // Agrupar por PreMeasurement e por Step
        Map<PreMeasurement, Map<Integer, List<PreMeasurementStreet>>> grouped =
                streets.stream().collect(
                        Collectors.groupingBy(
                                PreMeasurementStreet::getPreMeasurement,
                                Collectors.groupingBy(PreMeasurementStreet::getStep)
                        )
                );

        List<PreMeasurementResponseDTO> dtos = new ArrayList<>();

        grouped.forEach((preMeasurement, stepMap) -> {
            stepMap.forEach((step, streetList) -> {
                PreMeasurementResponseDTO dto = convertToPreMeasurementResponseDTO(preMeasurement, streetList, step);
                dtos.add(dto);
            });
        });

        return ResponseEntity.ok(dtos);
    }


    @Cacheable("getPreMeasurementById")
    public ResponseEntity<?> getPreMeasurementNotAssigned(long preMeasurementId, Integer step) {
        var streets = preMeasurementStreetRepository.getPreMeasurementNotAssignedById(preMeasurementId, step);

        return ResponseEntity.ok().body(convertToPreMeasurementResponseDTO(streets.getFirst().getPreMeasurement(), streets, step));
    }

    public PreMeasurementResponseDTO convertToPreMeasurementResponseDTO(PreMeasurement p, List<PreMeasurementStreet> streets, Integer step) {
        AtomicInteger number = new AtomicInteger(1);

        return new PreMeasurementResponseDTO(
                p.getPreMeasurementId(),
                p.getContract().getContractId(),
                p.getCity(),
                "",
                p.getTypePreMeasurement(),
                Objects.equals(p.getTypePreMeasurement(), ContractType.INSTALLATION) ? "badge-primary" : "badge-neutral",
                p.getTypePreMeasurement(),
                p.getTotalPrice() != null ? p.getTotalPrice().toString() : "0,00",
                p.getStatus(),
                step,
                streets.stream()
                        .filter(s -> !ItemStatus.CANCELLED.equals(s.getStreetStatus()))
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
                                s.getCreatedBy() != null ? s.getCreatedBy().getCompletedName() : "Desconhecido",
                                util.normalizeDate(s.getCreatedAt()),
                                s.getItems() != null
                                        ? s.getItems().stream()
                                        .filter(i -> !ItemStatus.CANCELLED.equals(i.getItemStatus()))
                                        .sorted(Comparator.comparing(PreMeasurementStreetItem::getPreMeasurementStreetItemId))
                                        .map(i -> new PreMeasurementStreetItemResponseDTO(
                                                i.getPreMeasurementStreetItemId(),
                                                i.getContractItem().getContractItemId(),
                                                i.getContractItem().getReferenceItem().getDescription(),
                                                i.getContractItem().getReferenceItem().getNameForImport(),
                                                i.getContractItem().getReferenceItem().getType(),
                                                i.getContractItem().getReferenceItem().getLinking(),
                                                i.getContractItem().getReferenceItem().getItemDependency(),
                                                i.getMeasuredItemQuantity(),
                                                i.getItemStatus()
                                        )).toList()
                                        : List.of()
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


        PreMeasurement preMeasurement = allStreets.getFirst().getPreMeasurement();
        preMeasurement.subtractTotalPrice(itemsPrices);
        preMeasurement.setStatus(ContractStatus.AVAILABLE);
        preMeasurementRepository.save(preMeasurement);
    }

    protected void cancelItems(List<CancelledItems> cancelledItems) {
        List<Long> changedItemsIds = cancelledItems.stream()
                .filter(Objects::nonNull)
                .map(CancelledItems::getItemId)
                .toList();

        List<PreMeasurementStreetItem> allItems = preMeasurementStreetItemRepository.findAllById(changedItemsIds)
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


        PreMeasurement preMeasurement = allItems.getFirst().getPreMeasurement();
        preMeasurement.subtractTotalPrice(itemsPrices);
        preMeasurement.setStatus(ContractStatus.AVAILABLE);
        preMeasurementRepository.save(preMeasurement);
    }

    protected void changeItems(List<ChangedItems> changedItems) {
        List<Long> changedItemsIds = changedItems.stream()
                .filter(Objects::nonNull)
                .map(ChangedItems::getItemId)
                .toList();

        // Carrega os itens antigos com base nos IDs
        List<PreMeasurementStreetItem> oldItems = preMeasurementStreetItemRepository.findAllById(changedItemsIds)
                .stream()
                .toList();

        Set<Long> preMeasurementId = oldItems.stream()
                .map(item -> item.getPreMeasurement().getPreMeasurementId())
                .collect(Collectors.toSet());

        if (preMeasurementId.size() > 1) {
            throw new IllegalStateException("Itens de múltiplas pré-medições não podem ser alterados juntos");
        }

        Map<Long, PreMeasurementStreetItem> itemMap = oldItems.stream()
                .collect(Collectors.toMap(PreMeasurementStreetItem::getPreMeasurementStreetItemId, i -> i));

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
                    Optional.ofNullable(item.getPreMeasurementStreet())
                            .ifPresent(s -> s.setStreetStatus(ItemStatus.APPROVED));
                })
                .map(Map.Entry::getKey)
                .toList();

//        continua depois...
//        for(var item : changedItems) {
//
//            if(item.getNewContractReferenceId() != -1) {
//                var sql = "UPDATE pre_measurement_street_item SET contract_item_id = :contract_item_id WHERE pre_measurement_street_item_id = ?";
//            }
//        }

        // Soma total de preço de itens novos
        BigDecimal newItemsPrice = newItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (newItems.isEmpty()) {
            throw new IllegalStateException("Nenhum item foi alterado.");
        }

        PreMeasurement preMeasurement = newItems.getFirst().getPreMeasurement();
        preMeasurement.subtractTotalPrice(oldItemsPrice);
        preMeasurement.sumTotalPrice(newItemsPrice.add(newItemsPrice));
        preMeasurement.setStatus(ContractStatus.AVAILABLE);
        preMeasurementRepository.save(preMeasurement);
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
                            street.getPreMeasurement().getContract(),
                            item,
                            street
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

    public ResponseEntity<?> saveStreetPhotos(List<MultipartFile> photos) {
        for (MultipartFile photo : photos) {
            try {
                var filename = photo.getOriginalFilename();
                if (filename == null || !filename.contains("#")) continue;

                var parts = filename.split("#");
                if (parts.length < 2) continue;

                Long deviceId = Long.parseLong(parts[0]);
                Long deviceStreetId = Long.parseLong(parts[1]);

                // Verifica se já existe uma foto salva
                String checkSql = "SELECT photo_uri FROM tb_pre_measurements_streets WHERE device_id = ? AND device_street_id = ?";
                try {
                    String existingUri = jdbcTemplate.queryForObject(checkSql, String.class, deviceId, deviceStreetId);
                    if (existingUri != null && !existingUri.isBlank()) {
                        // Já existe foto, pula essa
                        continue;
                    }
                } catch (EmptyResultDataAccessException e) {
                    // Não encontrou — segue para salvar
                }

                // Salva a nova foto
                String photoUri = minioService.uploadFile(photo, "scl-construtora", "photos", "rua");

                String updateSql = "UPDATE tb_pre_measurements_streets SET photo_uri = ? WHERE device_id = ? AND device_street_id = ?";
                jdbcTemplate.update(updateSql, photoUri, deviceId, deviceStreetId);

            } catch (Exception e) {
                e.printStackTrace(); // ou log.warn("Erro ao processar foto", e);
            }
        }

        return ResponseEntity.ok().build();
    }

}
