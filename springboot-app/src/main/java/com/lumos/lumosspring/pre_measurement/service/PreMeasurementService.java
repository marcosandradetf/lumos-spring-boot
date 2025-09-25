package com.lumos.lumosspring.pre_measurement.service;

import com.lumos.lumosspring.contract.repository.ContractRepository;
import com.lumos.lumosspring.dto.pre_measurement.PreMeasurementDTO;
import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreetItem;
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet;
import com.lumos.lumosspring.pre_measurement.jdbc.PreMeasurementJdbc;
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementRepository;
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetItemRepository;
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository;
import com.lumos.lumosspring.notifications.service.NotificationService;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

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
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PreMeasurementJdbc preMeasurementJdbc;

    public PreMeasurementService(PreMeasurementStreetRepository preMeasurementStreetRepository,
                                 MaterialRepository materialRepository,
                                 PreMeasurementRepository preMeasurementRepository,
                                 PreMeasurementStreetItemRepository preMeasurementStreetItemRepository,
                                 UserRepository userRepository, Util util,
                                 ContractRepository contractRepository,
                                 NotificationService notificationService,
                                 MinioService minioService, JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate, PreMeasurementJdbc preMeasurementJdbc) {
        this.preMeasurementStreetRepository = preMeasurementStreetRepository;
        this.preMeasurementRepository = preMeasurementRepository;
        this.preMeasurementStreetItemRepository = preMeasurementStreetItemRepository;
        this.userRepository = userRepository;
        this.util = util;
        this.contractRepository = contractRepository;
        this.notificationService = notificationService;
        this.minioService = minioService;
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.preMeasurementJdbc = preMeasurementJdbc;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getPreMeasurements", allEntries = true),
            @CacheEvict(cacheNames = "getPreMeasurementById", allEntries = true)
    })
    public ResponseEntity<?> markAsAvailable(Long preMeasurementId) {
        var status = preMeasurementRepository.getStatus(preMeasurementId);

        if(!Objects.equals(status, ExecutionStatus.PENDING)) {
            throw new Utils.BusinessException("Sua solicitação não pôde ser concluída, pois esta pré-medição não está mais disponível para análise. É possível que outro usuário já esteja trabalhando nela.");
        }

        preMeasurementJdbc.markAsAvailable(preMeasurementId);

        return ResponseEntity.noContent().build();
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
    public ResponseEntity<?> savePreMeasurement(PreMeasurementDTO preMeasurementDTO) {
        var userId = Utils.INSTANCE.getCurrentUserId();

        var contract = contractRepository.findContractByContractId(preMeasurementDTO.getContractId()).orElse(null);
        if (contract == null || !contract.getStatus().equals(ContractStatus.ACTIVE)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("O Contrato selecionado não está ativo no sistema."));
        }

        var step = contractRepository.getLastStep(preMeasurementDTO.getContractId());
        var preMeasurement = preMeasurementRepository.findByDevicePreMeasurementId(preMeasurementDTO.getPreMeasurementId())
                .orElseGet(() -> {
                    PreMeasurement newPre = new PreMeasurement();
                    newPre.setContractId(preMeasurementDTO.getContractId());
                    newPre.setTypePreMeasurement(ContractType.INSTALLATION);
                    newPre.setStatus(ExecutionStatus.PENDING);
                    newPre.setCity(contract.getContractor());
                    newPre.setStep(step + 1);
                    newPre.setDevicePreMeasurementId(preMeasurementDTO.getPreMeasurementId());
                    newPre.setCreatedByUserId(userId);
                    newPre.setCreatedAt(Instant.now());
                    return preMeasurementRepository.save(newPre);
                });

        var streets = preMeasurementDTO.getStreets();

        for (var streetDTO : streets) {
            PreMeasurementStreet preMeasurementStreet = new PreMeasurementStreet();
            var street = streetDTO.getStreet();

            var exists = preMeasurementStreetRepository.existsByDevicePreMeasurementStreetId(street.getPreMeasurementStreetId());
            if (exists) {
                continue;
            }

            preMeasurementStreet.setDevicePreMeasurementStreetId(street.getPreMeasurementStreetId());
            preMeasurementStreet.setPreMeasurementId(preMeasurement.getPreMeasurementId());
            preMeasurementStreet.setAddress(street.getAddress());
            preMeasurementStreet.setLatitude(street.getLatitude());
            preMeasurementStreet.setLongitude(street.getLongitude());
            preMeasurementStreet.setLastPower(street.getLastPower());
            preMeasurementStreet.setStreetStatus(ItemStatus.PENDING);

            preMeasurementStreet = preMeasurementStreetRepository.save(preMeasurementStreet);
            final var savedStreet = preMeasurementStreet; // agora é effectively final

            for (var itemDTO : streetDTO.getItems()) {
                namedParameterJdbcTemplate.query("""
                                    select ci.contract_item_id
                                    from contract_reference_item cri
                                    join contract_item ci on ci.contract_item_reference_id = cri.contract_reference_item_id
                                    where cri.contract_reference_item_id = :contractReferenceItemId and ci.contract_contract_id = :contractId
                                """,
                        Map.of("contractReferenceItemId", itemDTO.getContractReferenceItemId(),
                                "contractId", preMeasurement.getContractId()),
                        (rs) -> {
                            var newItem = new PreMeasurementStreetItem();
                            newItem.setPreMeasurementStreetId(savedStreet.getPreMeasurementStreetId());
                            newItem.setPreMeasurementId(preMeasurement.getPreMeasurementId());
                            newItem.setItemStatus(ItemStatus.PENDING);
                            newItem.setMeasuredItemQuantity(itemDTO.getMeasuredQuantity());
                            newItem.setContractItemId(rs.getLong("contract_item_id"));
                            preMeasurementStreetItemRepository.save(newItem);
                        });
            }
        }

        var allItems = preMeasurementStreetItemRepository.findAllByPreMeasurementId(preMeasurement.getPreMeasurementId());
        BigDecimal itemsPrices = allItems.stream()
                .map(PreMeasurementStreetItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        preMeasurement.setTotalPrice(itemsPrices);
        preMeasurementRepository.save(preMeasurement);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> saveStreetPhotos(List<MultipartFile> photos) {
        for (MultipartFile photo : photos) {
            try {
                var filename = photo.getOriginalFilename();
                if (filename == null) continue;

                var parts = filename.split("#");
                if (parts.length < 1) continue;

                UUID deviceStreetId;

                try {
                    deviceStreetId = UUID.fromString(parts[0]);
                } catch (Exception _) {
                    continue;
                }

                // Verifica se já existe uma foto salva
                String existingUri = null;
                try {
                    existingUri = namedParameterJdbcTemplate.queryForObject(
                            """
                                         SELECT pre_measurement_photo_uri
                                         FROM pre_measurement_street
                                         WHERE device_pre_measurement_street_id = :deviceStreetId
                                    """,
                            Map.of("deviceStreetId", deviceStreetId),
                            String.class
                    );
                } catch (EmptyResultDataAccessException _) {
                    // nenhum registro encontrado → existingUri fica null
                }


                if (existingUri != null) {
                    continue;
                }

                // Salva a nova foto
                String photoUri = minioService.uploadFile(photo, "scl-construtora", "photos/pre_measurement", "ponto");

                String updateSql = "UPDATE pre_measurement_street SET pre_measurement_photo_uri = ? WHERE device_pre_measurement_street_id = ?";
                jdbcTemplate.update(updateSql, photoUri, deviceStreetId);

            } catch (Exception e) {
                e.printStackTrace(); // ou log.warn("Erro ao processar foto", e);
            }
        }

        return ResponseEntity.ok().build();
    }


    @Cacheable("getPreMeasurements")
    public ResponseEntity<?> getAll(String status) {

        return ResponseEntity.ok(preMeasurementJdbc.findAllByStatus(status));
    }

    public ResponseEntity<?> checkBalance(Long preMeasurementId) {
        return ResponseEntity.ok(preMeasurementJdbc.checkBalance(preMeasurementId));
    }

    @Cacheable("getPreMeasurementById")
    public ResponseEntity<?> findById(long preMeasurementID) {
        return ResponseEntity.ok(preMeasurementJdbc.findById(preMeasurementID));
    }

//    /**
//     * MÉTODO PARA SALVAR AS MODIFICAÇOES NA PRÉ-MEDIÇÃO
//     */
//    @Caching(evict = {
//            @CacheEvict(cacheNames = "getPreMeasurements", allEntries = true),
//            @CacheEvict(cacheNames = "getPreMeasurementById", allEntries = true)
//    })
//    @Transactional
//    public ResponseEntity<?> saveModifications(ModificationsDTO modificationsDTO) {
//        var cancelledStreets = modificationsDTO.getCancelledStreets();
//        var cancelledItems = modificationsDTO.getCancelledItems();
//        var changedItems = modificationsDTO.getChangedItems();
//
//        if (!cancelledStreets.isEmpty())
//            cancelStreets(cancelledStreets);
//
//        if (!cancelledItems.isEmpty())
//            cancelItems(cancelledItems);
//
//        if (!changedItems.isEmpty())
//            changeItems(changedItems);
//
//
//        return ResponseEntity.ok(new DefaultResponse("Itens Atualizados com Sucesso!"));
//    }
//
//    protected void cancelStreets(List<CancelledStreets> cancelledStreets) {
//        List<Long> streetIds = cancelledStreets.stream()
//                .filter(Objects::nonNull)
//                .map(CancelledStreets::getStreetId)
//                .toList();
//
//        List<PreMeasurementStreet> allStreets = preMeasurementStreetRepository.findAllById(streetIds)
//                .stream()
//                .peek(s -> {
//                    s.setStreetStatus(ItemStatus.CANCELLED);
//                    if (s.getItems() != null) {
//                        s.getItems().forEach(item -> item.setItemStatus(ItemStatus.CANCELLED));
//                    }
//                })
//                .toList();
//
//        Set<Long> preMeasurementIds = allStreets.stream()
//                .map(s -> s.getPreMeasurement().getPreMeasurementId())
//                .collect(Collectors.toSet());
//
//        if (preMeasurementIds.size() > 1) {
//            throw new IllegalStateException("Ruas de múltiplas pré-medições não podem ser canceladas juntas");
//        }
//
//        BigDecimal itemsPrices = allStreets.stream()
//                .map(PreMeasurementStreet::getItems)
//                .filter(Objects::nonNull)
//                .flatMap(Collection::stream)
//                .map(PreMeasurementStreetItem::getTotalPrice)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//
//        PreMeasurement preMeasurement = allStreets.getFirst().getPreMeasurement();
//        preMeasurement.subtractTotalPrice(itemsPrices);
//        preMeasurement.setStatus(ExecutionStatus.AVAILABLE);
//        preMeasurementRepository.save(preMeasurement);
//    }
//
//    protected void cancelItems(List<CancelledItems> cancelledItems) {
//        List<Long> changedItemsIds = cancelledItems.stream()
//                .filter(Objects::nonNull)
//                .map(CancelledItems::getItemId)
//                .toList();
//
//        List<PreMeasurementStreetItem> allItems = preMeasurementStreetItemRepository.findAllById(changedItemsIds)
//                .stream()
//                .peek(s ->
//                        s.setItemStatus(ItemStatus.CANCELLED)
//                )
//                .toList();
//
//        Set<Long> preMeasurementIds = allItems.stream()
//                .map(item -> item.getPreMeasurement().getPreMeasurementId())
//                .collect(Collectors.toSet());
//
//        if (preMeasurementIds.size() > 1) {
//            throw new IllegalStateException("Itens de múltiplas pré-medições não podem ser cancelados juntos");
//        }
//
//        BigDecimal itemsPrices = allItems.stream()
//                .map(PreMeasurementStreetItem::getTotalPrice)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//
//        PreMeasurement preMeasurement = allItems.getFirst().getPreMeasurement();
//        preMeasurement.subtractTotalPrice(itemsPrices);
//        preMeasurement.setStatus(ExecutionStatus.AVAILABLE);
//        preMeasurementRepository.save(preMeasurement);
//    }
//
//    protected void changeItems(List<ChangedItems> changedItems) {
//        List<Long> changedItemsIds = changedItems.stream()
//                .filter(Objects::nonNull)
//                .map(ChangedItems::getItemId)
//                .toList();
//
//        // Carrega os itens antigos com base nos IDs
//        List<PreMeasurementStreetItem> oldItems = preMeasurementStreetItemRepository.findAllById(changedItemsIds)
//                .stream()
//                .toList();
//
//        Set<Long> preMeasurementId = oldItems.stream()
//                .map(item -> item.getPreMeasurement().getPreMeasurementId())
//                .collect(Collectors.toSet());
//
//        if (preMeasurementId.size() > 1) {
//            throw new IllegalStateException("Itens de múltiplas pré-medições não podem ser alterados juntos");
//        }
//
//        Map<Long, PreMeasurementStreetItem> itemMap = oldItems.stream()
//                .collect(Collectors.toMap(PreMeasurementStreetItem::getPreMeasurementStreetItemId, i -> i));
//
//        // Soma total de preço de itens antigos
//        BigDecimal oldItemsPrice = oldItems.stream()
//                .map(PreMeasurementStreetItem::getTotalPrice)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Atualiza os itens com as novas quantidades e preços
//        var newItems = changedItems.stream()
//                .map(cs -> Map.entry(itemMap.get(cs.getItemId()), cs.getQuantity()))
//                .filter(entry -> entry.getKey() != null)
//                .peek(entry -> {
//                    var item = entry.getKey();
//                    var quantity = entry.getValue();
//
//                    item.setItemStatus(ItemStatus.APPROVED);
//                    item.setMeasuredItemQuantity(quantity);
//                    item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
//                    Optional.ofNullable(item.getPreMeasurementStreet())
//                            .ifPresent(s -> s.setStreetStatus(ItemStatus.APPROVED));
//                })
//                .map(Map.Entry::getKey)
//                .toList();
//
////        continua depois...
////        for(var item : changedItems) {
////
////            if(item.getNewContractReferenceId() != -1) {
////                var sql = "UPDATE pre_measurement_street_item SET contract_item_id = :contract_item_id WHERE pre_measurement_street_item_id = ?";
////            }
////        }
//
//        // Soma total de preço de itens novos
//        BigDecimal newItemsPrice = newItems.stream()
//                .map(PreMeasurementStreetItem::getTotalPrice)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        if (newItems.isEmpty()) {
//            throw new IllegalStateException("Nenhum item foi alterado.");
//        }
//
//        PreMeasurement preMeasurement = newItems.getFirst().getPreMeasurement();
//        preMeasurement.subtractTotalPrice(oldItemsPrice);
//        preMeasurement.sumTotalPrice(newItemsPrice.add(newItemsPrice));
//        preMeasurement.setStatus(ExecutionStatus.AVAILABLE);
//        preMeasurementRepository.save(preMeasurement);
//    }
//
//    @Transactional
//    public ResponseEntity<?> importPreMeasurements(PreMeasurementDTO preMeasurement, String userUUID) {
//        return this.savePreMeasurement(preMeasurement, userUUID);
//    }
//
//    @Transactional
//    public ResponseEntity<?> deletePreMeasurementStreets(DeletePreMeasurementDTO deleteDTO) {
//        this.preMeasurementStreetItemRepository.deleteByStreet(deleteDTO.getPreMeasurementStreetIds());
//        this.preMeasurementStreetRepository.deleteByStreet(deleteDTO.getPreMeasurementStreetIds());
//
//        var preMeasurement = this.preMeasurementRepository.findByPreMeasurementId(deleteDTO.getPreMeasurementId());
//        if (preMeasurement == null) {
//            return ResponseEntity.notFound().build();
//        }
//        updatePremeasurementPrice(preMeasurement);
//
//        return ResponseEntity.ok().build();
//    }
//
//    @Transactional
//    public ResponseEntity<?> deleteProject(DeletePreMeasurementDTO deleteDTO) {
//
//        var streets = preMeasurementStreetRepository.findByIds(deleteDTO.getPreMeasurementStreetIds());
//        if (streets.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        for (PreMeasurementStreet street : streets) {
//            for (PreMeasurementStreetItem item : street.getItems()) {
//                if (item.getContractServiceIdMask() != null) {
//                    item.clearContractServices();
//                    insertServices(
//                            street.getPreMeasurement().getContract(),
//                            item,
//                            street
//                    );
//                }
//            }
//        }
//        preMeasurementStreetRepository.saveAll(streets);
//
//
//        return ResponseEntity.ok().build();
//    }
//
//    private void updatePremeasurementPrice(PreMeasurement preMeasurement) {
//        var allItems = preMeasurementStreetItemRepository.findAllByPreMeasurement(preMeasurement);
//
//        BigDecimal itemsPrices = allItems.stream()
//                .map(PreMeasurementStreetItem::getTotalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal servicesPrices = allItems.stream()
//                .map(PreMeasurementStreetItem::getContractServiceDividerPrices)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        preMeasurement.setTotalPrice(itemsPrices.add(servicesPrices));
//        preMeasurementRepository.save(preMeasurement);
//    }
//


}
