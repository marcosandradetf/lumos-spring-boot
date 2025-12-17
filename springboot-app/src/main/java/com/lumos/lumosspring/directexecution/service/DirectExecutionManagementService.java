package com.lumos.lumosspring.directexecution.service;

import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository;
import com.lumos.lumosspring.contract.repository.ContractRepository;
import com.lumos.lumosspring.contract.service.ContractService;
import com.lumos.lumosspring.directexecution.dto.DirectExecutionDTO;
import com.lumos.lumosspring.directexecution.model.DirectExecution;
import com.lumos.lumosspring.directexecution.model.DirectExecutionItem;
import com.lumos.lumosspring.directexecution.repository.DirectExecutionRepository;
import com.lumos.lumosspring.directexecution.repository.DirectExecutionRepositoryItem;
import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.notifications.service.NotificationService;
import com.lumos.lumosspring.stock.order.installationrequest.model.ReservationManagement;
import com.lumos.lumosspring.stock.order.installationrequest.repository.ReservationManagementRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class DirectExecutionManagementService {
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final ReservationManagementRepository reservationManagementRepository;
    private final DirectExecutionRepository directExecutionRepository;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final ContractService contractService;
    private final NotificationService notificationService;
    private final ContractItemsQuantitativeRepository contractItemsQuantitativeRepository;
    private final DirectExecutionRepositoryItem directExecutionItemRepository;
    private final MinioService minioService;

    public DirectExecutionManagementService(UserRepository userRepository, ContractRepository contractRepository, ReservationManagementRepository reservationManagementRepository, DirectExecutionRepository directExecutionRepository, NamedParameterJdbcTemplate namedJdbc, ContractService contractService, NotificationService notificationService, ContractItemsQuantitativeRepository contractItemsQuantitativeRepository, DirectExecutionRepositoryItem directExecutionItemRepository, MinioService minioService) {
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
        this.reservationManagementRepository = reservationManagementRepository;
        this.directExecutionRepository = directExecutionRepository;
        this.namedJdbc = namedJdbc;
        this.contractService = contractService;
        this.notificationService = notificationService;
        this.contractItemsQuantitativeRepository = contractItemsQuantitativeRepository;
        this.directExecutionItemRepository = directExecutionItemRepository;
        this.minioService = minioService;
    }

    @Transactional
    public ResponseEntity<Object> delegateDirectExecution(DirectExecutionDTO execution) {
        // 1) Estoquista
        var stockist = userRepository.findByUserId(execution.getStockistId()).orElse(null);
        if (stockist == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DefaultResponse("Estoquista não encontrado"));
        }

        // 2) Contrato
        var contract = contractRepository.findById(execution.getContractId()).orElse(null);
        if (contract == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DefaultResponse("Contrato não encontrado"));
        }

        // 3) Usuário corrente (quem delega)
        UUID currentUserUUID = execution.getCurrentUserId();

        // 4) Passo/etapa atual
        int step = contractRepository.getLastStep(execution.getContractId()) + 1;

        // 5) Cria o gerenciamento de reserva (ReservationManagement)
        var management = new ReservationManagement(
                null,
                STR."Etapa \{step} - \{contract.getContractor()}",
                stockist.getUserId(),
                ReservationStatus.PENDING
        );
        management = reservationManagementRepository.save(management);

        Long managementId = management.getReservationManagementId();
        if (managementId == null) {
            throw new IllegalStateException("Execution Service - reservationManagementId não foi gerado!");
        }

        // 6) Cria a execução direta
        var directExecution = new DirectExecution(
                null,
                "Etapa " + step + " - " + contract.getContractor(),
                execution.getInstructions(),
                execution.getContractId(),
                ExecutionStatus.WAITING_STOCKIST,
                execution.getTeamId(),
                currentUserUUID,
                Instant.now(),
                managementId,
                step
        );


        directExecution = directExecutionRepository.save(directExecution);
        Long directExecutionId = directExecution.getDirectExecutionId();
        if (directExecutionId == null) {
            throw new IllegalStateException("Id da execução não encontrado");
        }

        // 7) Para cada item solicitado na execução
        for (var item : execution.getItems()) {
            // 7.1) Busca informações do item de contrato
            List<Map<String, Object>> ciqList = contractService.queryContractItems(contract.getContractId());
            Map<String, Object> ciq = ciqList.stream()
                    .filter(map -> Objects.equals(asLong(map.get("contract_Item_Id")), item.getContractItemId()))
                    .findFirst()
                    .orElse(null);

            // 7.2) Descrição do item (name_for_import) a partir do referenceId
            String description = null;
            if (ciq != null) {
                Long referenceId = asLong(ciq.get("contract_reference_item_id"));
                Map<String, Object> descriptionRow = JdbcUtil.INSTANCE.getSingleRow(
                        namedJdbc,
                        """
                                select name_for_import
                                from contract_reference_item
                                where contract_reference_item_id = :referenceId
                                """,
                        Map.of("referenceId", referenceId != null ? referenceId : -1L)
                );
                if (descriptionRow != null) {
                    description = Objects.toString(descriptionRow.get("name_for_import"), null);
                }
            }

            if (ciq != null) {
                // 7.3) Verifica saldo contratual disponível
                BigDecimal contractedQuantity = toBigDecimal(ciq.get("contracted_quantity"));
                BigDecimal executedQuantity = toBigDecimal(ciq.get("quantity_executed"));
                BigDecimal available = contractedQuantity.subtract(executedQuantity);

                if (available.compareTo(item.getQuantity()) < 0) {
                    throw new IllegalStateException("Não há saldo disponível para o item " + (description != null ? description : ""));
                }

                // 7.5) Cria o item da execução direta
                String type = (ciq.get("type") != null) ? ciq.get("type").toString() : "";
                String itemStatus = Set.of("SERVIÇO", "PROJETO").contains(type)
                        ? ReservationStatus.FINISHED
                        : ReservationStatus.PENDING;

                var directExecutionItem = new DirectExecutionItem(
                        null,
                        item.getQuantity(),
                        item.getContractItemId(),
                        directExecutionId,
                        itemStatus
                );

                directExecutionItemRepository.save(directExecutionItem);
            }
        }

        // 8) Notifica o estoquista responsável
        notificationService.sendNotificationForTopic(
                "Nova Ordem Disponível",
                "Uma nova ordem foi atribuída a você. Acesse a tela de Gerenciamento de Reservas para iniciar o processo.",
                null,
                execution.getStockistId().toString(),
                Instant.now(),
                NotificationType.ALERT
        );

        return ResponseEntity.ok(new DefaultResponse(step + " etapa criada com sucesso"));
    }

    @Transactional
    public ResponseEntity<Object> cancelStep(Map<String, Object> payLoad) {
        var ids = Optional.ofNullable(payLoad.get("currentIds"))
                .filter(List.class::isInstance)
                .map(raw -> (List<?>) raw)
                .map(list -> list.stream()
                        .map(o -> (o instanceof Number n) ? n.longValue() : null)
                        .filter(Objects::nonNull)
                        .toList()
                )
                .orElseGet(List::of);


        var type = (String) payLoad.get("type");

        try {
            if (Objects.equals(type, "DIRECT_EXECUTION")) {
                namedJdbc.update(
                        """
                                    delete from direct_execution_item
                                    where direct_execution_id in (:ids)
                                """,
                        Map.of("ids", ids)
                );

                namedJdbc.query(
                        """
                                        delete from direct_execution
                                        where direct_execution_id in (:ids)
                                        returning reservation_management_id
                                """,
                        Map.of("ids", ids),
                        (rs) -> {
                            var id = rs.getLong("reservation_management_id");
                            namedJdbc.update(
                                    """
                                                delete from reservation_management
                                                where reservation_management.reservation_management_id = :id
                                            """,
                                    Map.of("id", id)
                            );
                        }
                );


            } else {
                throw new Utils.BusinessException("Exclusão não implementada para instalações com pré-medição - Comunique ao fabricante do sistema.");
            }

        } catch (DataIntegrityViolationException e){
            throw new Utils.BusinessException(e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }

    @Transactional
    public ResponseEntity<Object> archiveOrDelete(Map<String, Object> payload) {
        var directExecutionId = ((Number) payload.get("directExecutionId")).longValue();
        var action = (String) payload.get("action");

        if(action == null) {
            throw new Utils.BusinessException("Tente novamente - ação não recebida");
        }

        if (action.equals("ARCHIVE")) {
            namedJdbc.update(
                    """
                                update direct_execution
                                set direct_execution_status = 'ARCHIVED'
                                WHERE direct_execution_id = :directExecutionId
                            """,
                    Map.of("directExecutionId", directExecutionId)
            );
        } else {
            Set<String> uriObject = new HashSet<>();

            namedJdbc.query(
                    """
                                select desi.material_stock_id, desi.contract_item_id, desi.executed_quantity, des.execution_photo_uri
                                from direct_execution_street_item desi
                                join direct_execution_street des on des.direct_execution_street_id = desi.direct_execution_street_id
                                where des.direct_execution_id = :directExecutionId
                            """,
                    Map.of("directExecutionId", directExecutionId),
                    (rs) -> {
                        var materialStockId = rs.getLong("material_stock_id");
                        var contractItemId = rs.getLong("contract_item_id");
                        var executedQuantity = rs.getBigDecimal("executed_quantity");
                        var photoUri = rs.getString("execution_photo_uri");

                        if (photoUri != null && !photoUri.isEmpty()) {
                            uriObject.add(photoUri);
                        }

                        namedJdbc.update(
                                """
                                            update material_stock
                                            set stock_quantity = stock_quantity + :quantity_executed,
                                                stock_available = stock_available + :quantity_executed
                                            where material_id_stock = :material_stock_id
                                        """,
                                Map.of(
                                        "material_stock_id", materialStockId,
                                        "quantity_executed", executedQuantity
                                )
                        );

                        namedJdbc.update(
                                """
                                            update contract_item
                                            set quantity_executed = quantity_executed - :quantity_executed
                                            where contract_item_id = :contract_item_id
                                        """,
                                Map.of(
                                        "contract_item_id", contractItemId,
                                        "quantity_executed", executedQuantity
                                )
                        );
                    }
            );

            namedJdbc.update(
                    """
                                DELETE FROM direct_execution_street_item desi
                                USING direct_execution_street des
                                WHERE des.direct_execution_id = :direct_execution_id
                                    AND des.direct_execution_street_id = desi.direct_execution_street_id
                            """,
                    Map.of("direct_execution_id", directExecutionId)
            );

            namedJdbc.update(
                    """
                                delete from material_reservation
                                WHERE direct_execution_id = :direct_execution_id
                            """,
                    Map.of("direct_execution_id", directExecutionId)
            );

            minioService.deleteFiles(Utils.INSTANCE.getCurrentBucket(), uriObject);

            namedJdbc.update(
                    """
                                DELETE FROM direct_execution_street
                                WHERE direct_execution_id = :direct_execution_id
                            """,
                    Map.of("direct_execution_id", directExecutionId)
            );

            namedJdbc.update(
                    """
                                DELETE FROM direct_execution 
                                WHERE direct_execution_id = :direct_execution_id
                            """,
                    Map.of("direct_execution_id", directExecutionId)
            );

            namedJdbc.update(
                    """
                                delete from direct_execution_executor
                                WHERE direct_execution_id = :maintenanceId
                            """,
                    Map.of("direct_execution_id", directExecutionId)
            );
        }

        return ResponseEntity.noContent().build();
    }

    /* ================= Helpers ================= */

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Object o) {
        switch (o) {
            case null -> {
                return BigDecimal.ZERO;
            }
            case BigDecimal bd -> {
                return bd;
            }
            case Number n -> {
                return new BigDecimal(n.toString());
            }
            default -> {
            }
        }
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

}
