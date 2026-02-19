package com.lumos.lumosspring.serviceorder.service.installation;

import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository;
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecution;
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepository;
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryItem;
import com.lumos.lumosspring.installation.repository.premeasurement.PreMeasurementInstallationRepository;
import com.lumos.lumosspring.installation.repository.view.InstallationViewRepository;
import com.lumos.lumosspring.premeasurement.repository.PreMeasurementStreetRepository;
import com.lumos.lumosspring.installation.model.premeasurement.PreMeasurement;
import com.lumos.lumosspring.premeasurement.repository.PreMeasurementRepository;
import com.lumos.lumosspring.premeasurement.repository.PreMeasurementStreetItemRepository;
import com.lumos.lumosspring.stock.materialsku.model.Material;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialstock.model.MaterialStock;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.serviceorder.dto.installation.ReserveDTOCreate;
import com.lumos.lumosspring.serviceorder.model.installation.MaterialReservation;
import com.lumos.lumosspring.serviceorder.repository.installation.MaterialReservationRepository;
import com.lumos.lumosspring.serviceorder.repository.installation.ReservationManagementRepository;
import com.lumos.lumosspring.util.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


@Service
public class ManagementRegisterService {
    private final NamedParameterJdbcTemplate namedJdbc;
    private final PreMeasurementRepository preMeasurementRepository;
    private final DirectExecutionRepository directExecutionRepository;
    private final ContractItemsQuantitativeRepository contractItemsQuantitativeRepository;
    private final MaterialReservationRepository materialReservationRepository;
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final MaterialReferenceRepository materialReferenceRepository;
    private final ReservationManagementRepository reservationManagementRepository;
    private final PreMeasurementStreetItemRepository preMeasurementStreetItemRepository;
    private final DirectExecutionRepositoryItem directExecutionRepositoryItem;
    private final InstallationViewRepository installationViewRepository;
    private final PreMeasurementInstallationRepository preMeasurementInstallationRepository;
    private final PreMeasurementStreetRepository preMeasurementStreetRepository;

    public ManagementRegisterService(
            NamedParameterJdbcTemplate namedJDBC,
            PreMeasurementRepository preMeasurementRepository,
            DirectExecutionRepository directExecutionRepository,
            ContractItemsQuantitativeRepository contractItemsQuantitativeRepository,
            MaterialReservationRepository materialReservationRepository,
            MaterialStockRegisterRepository materialStockRegisterRepository,
            MaterialReferenceRepository materialReferenceRepository,
            ReservationManagementRepository reservationManagementRepository,
            PreMeasurementStreetItemRepository preMeasurementStreetItemRepository,
            DirectExecutionRepositoryItem directExecutionRepositoryItem,
            InstallationViewRepository installationViewRepository,
            PreMeasurementInstallationRepository preMeasurementInstallationRepository, PreMeasurementStreetRepository preMeasurementStreetRepository
    ) {
        this.namedJdbc = namedJDBC;
        this.preMeasurementRepository = preMeasurementRepository;
        this.directExecutionRepository = directExecutionRepository;
        this.contractItemsQuantitativeRepository = contractItemsQuantitativeRepository;
        this.materialReservationRepository = materialReservationRepository;
        this.materialStockRegisterRepository = materialStockRegisterRepository;
        this.materialReferenceRepository = materialReferenceRepository;
        this.reservationManagementRepository = reservationManagementRepository;
        this.preMeasurementStreetItemRepository = preMeasurementStreetItemRepository;
        this.directExecutionRepositoryItem = directExecutionRepositoryItem;
        this.installationViewRepository = installationViewRepository;
        this.preMeasurementInstallationRepository = preMeasurementInstallationRepository;
        this.preMeasurementStreetRepository = preMeasurementStreetRepository;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getPreMeasurementInstallations", key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"),
            @CacheEvict(cacheNames = "getDirectExecutionInstallations", key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"),
    })
    @Transactional
    public ResponseEntity<?> reserveMaterialsForExecution(ReserveDTOCreate executionReserve) {
        var userUUID = Utils.getCurrentUserId();
        var management = reservationManagementRepository.findById(executionReserve.getReservationManagementId())
                .orElseThrow();

        if (!management.getStockistId().equals(userUUID)) {
            throw new Utils.BusinessException("Essa ordem de serviço não está mais atribuída para você");
        } else if (!management.getStatus().equals("PENDING")) {
            throw new Utils.BusinessException("Essa ordem de serviço não está mais pendente ou está finalizada.");
        }

        // 1) Busca pré-medição (se houver) e valida status
        Long preMeasurementID = executionReserve.getPreMeasurementId();
        PreMeasurement preMeasurement = null;

        if (preMeasurementID != null) {
            preMeasurement = preMeasurementRepository.findById(preMeasurementID)
                    .orElseThrow(() -> new IllegalStateException(STR."Street not found for id: \{preMeasurementID}"));

            if (!Objects.equals(preMeasurement.getStatus(), ExecutionStatus.WAITING_STOCKIST)) {
                return ResponseEntity.status(500)
                        .body(new DefaultResponse("Os itens dessa execução já foram todos reservados, inicie a próxima etapa."));
            }
        }

        // 2) Busca execução direta (se houver)
        DirectExecution directExecution = null;
        if (executionReserve.getDirectExecutionId() != null) {
            directExecution = directExecutionRepository.findById(executionReserve.getDirectExecutionId())
                    .orElseThrow(() -> new IllegalArgumentException("Execução não encontrada: " + executionReserve.getDirectExecutionId()));
        }

        // 4) Descobrir o depósito da equipe
        Map<String, Object> depositRow = JdbcUtil.INSTANCE.getSingleRow(
                namedJdbc,
                """
                        SELECT deposit_id_deposit
                        FROM team
                        WHERE id_team = :teamId
                        """,
                Map.of("teamId", executionReserve.getTeamId())
        );

        if (depositRow == null || !depositRow.containsKey("deposit_id_deposit")) {
            throw new IllegalStateException("Deposit Id not found");
        }

        Object depVal = depositRow.get("deposit_id_deposit");
        long depositId = (depVal instanceof Number n) ? n.longValue()
                : Long.parseLong(String.valueOf(depVal)); // fallback se vier como String

        // 5) Processa reservas
        List<MaterialReservation> reservations = new ArrayList<>();

        for (var item : executionReserve.getItems()) {
            long contractItemId = item.getContractItemId();

            for (var materialReserve : item.getMaterials()) {
                if (!materialReserve.getTruckStockControl()) {
                    continue;
                }

                // Quantidade disponível do item (saldo contratual)
                BigDecimal available = contractItemsQuantitativeRepository.getTotalBalance(contractItemId);
                BigDecimal requested = materialReserve.getMaterialQuantity();

                if (available.compareTo(requested) < 0) {
                    // Há reservas em andamento que podem estourar o saldo?
                    var inProgress = contractItemsQuantitativeRepository.getInProgressReservations(contractItemId);

                    if (inProgress == null || inProgress.isEmpty()) {
                        throw new Utils.BusinessException(
                                STR."Apesar de no sistema existir saldo para o item \{contractItemId} existem execuções em andamentos que podem fazer o saldo estourar"
                        );
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("O sistema mostra saldo disponível para o item ")
                            .append(inProgress.getFirst().itemName())
                            .append(", porém existem reservas para instalações em andamento que podem causar estouro no saldo.")
                            .append("\n\nInstalações afetadas:\n");

                    for (var r : inProgress) {
                        sb.append(r.description()).append('\n')
                                .append("  - Quantidade Reservada: ").append(r.reservedQuantity()).append('\n')
                                .append("  - Quantidade Completada: ").append(r.quantityCompleted()).append('\n')
                                .append("___________________\n");
                    }

                    BigDecimal currentBalance = contractItemsQuantitativeRepository.getTotalBalance(contractItemId);
                    sb.append("\nSaldo atual: ").append(currentBalance);

                    throw new Utils.BusinessException(sb.toString());
                }

                // Monta a reserva
                MaterialReservation reservation;

                if (materialReserve.getCentralMaterialStockId() != null) {
                    // Central (depósito central / caminhão do estoquista)
                    MaterialStock materialStock = materialStockRegisterRepository
                            .findById(materialReserve.getCentralMaterialStockId())
                            .orElse(null);

                    if (materialStock == null) {
                        throw new IllegalStateException("Material não encontrado");
                    }

                    Material materialRef = materialReferenceRepository
                            .findById(materialStock.getMaterialId())
                            .orElseThrow();

                    // Checa estoque central disponível
                    BigDecimal stockAvailable = materialStock.getStockAvailable();
                    if (stockAvailable.compareTo(requested) < 0) {
                        throw new IllegalArgumentException(
                                "O material " + materialRef.getMaterialName() + " não possuí estoque suficiente"
                        );
                    }

                    // Verifica se o estoquista logado atende o material (estoquista x material)
                    boolean stockistMatch = JdbcUtil.INSTANCE.existsRaw(
                            namedJdbc,
                            """
                                    SELECT 1
                                    FROM material_stock ms
                                    INNER JOIN stockist s ON s.deposit_id_deposit = ms.deposit_id
                                    WHERE ms.material_id_stock = :materialId
                                      AND s.user_id_user     = :userId
                                    LIMIT 1
                                    """,
                            Map.of(
                                    "materialId", materialReserve.getCentralMaterialStockId(),
                                    "userId", userUUID
                            )
                    );

                    String status;
                    if (stockistMatch) {
                        status = ReservationStatus.APPROVED;
                    } else {
                        status = ReservationStatus.PENDING;
                    }

                    // Localiza o estoque do caminhão/depósito do time
                    Map<String, Object> truckRow = JdbcUtil.INSTANCE.getSingleRow(
                            namedJdbc,
                            """
                                    SELECT material_id_stock
                                    FROM material_stock
                                    WHERE material_id = :materialId
                                      AND deposit_id  = :depositId
                                    """,
                            Map.of(
                                    "materialId", materialReserve.getMaterialId(),
                                    "depositId", depositId
                            )
                    );

                    if (truckRow == null || !truckRow.containsKey("material_id_stock")) {
                        throw new IllegalStateException(
                                STR."Truck Material stock not found \{materialReserve.getMaterialId()} - deposit: \{depositId}"
                        );
                    }

                    Object truckVal = truckRow.get("material_id_stock");
                    Long truckMaterialStockId = (truckVal instanceof Number tn) ? tn.longValue()
                            : Long.parseLong(String.valueOf(truckVal));

                    String desc = (preMeasurement != null)
                            ? (preMeasurement.getStep() + " etapa - " + preMeasurement.getCity())
                            : "Reserva de execução";

                    reservation = new MaterialReservation(
                            null,
                            desc,
                            materialReserve.getCentralMaterialStockId(),
                            truckMaterialStockId,
                            executionReserve.getPreMeasurementId(),
                            executionReserve.getDirectExecutionId(),
                            contractItemId,
                            requested,
                            BigDecimal.ZERO,
                            status,
                            executionReserve.getTeamId(),
                            Instant.now(),
                            userUUID,
                            null,
                            null,
                            null

                    );

                } else {
                    // Reserva direta para o caminhão (já em estoque do time)
                    Long truckMaterialStockId = materialReserve.getTruckMaterialStockId();
                    if (truckMaterialStockId == null) {
                        throw new IllegalStateException("Truck Material stock Id not found");
                    }

                    String desc = (preMeasurement != null)
                            ? (preMeasurement.getStep() + " etapa - " + preMeasurement.getCity())
                            : "Reserva de execução";

                    reservation = new MaterialReservation(
                            null,
                            desc,
                            materialReserve.getCentralMaterialStockId(),
                            truckMaterialStockId,
                            executionReserve.getPreMeasurementId(),
                            executionReserve.getDirectExecutionId(),
                            contractItemId,
                            requested,
                            BigDecimal.ZERO,
                            ReservationStatus.IN_STOCK,
                            executionReserve.getTeamId(),
                            Instant.now(),
                            userUUID,
                            null,
                            null,
                            null
                    );

                }

                reservations.add(reservation);
            }
        }

        // 6) Persiste todas as reservas
        materialReservationRepository.saveAll(reservations);

        // 7) Mensagem de retorno
        String responseMessage =
                (preMeasurement != null) ? verifyStreetReservations(reservations, preMeasurement)
                        : (directExecution != null) ? verifyDirectExecutionReservations(reservations, directExecution)
                        : "";

        return ResponseEntity.ok(new DefaultResponse(responseMessage));
    }

    @Transactional
    protected String verifyStreetReservations(
            List<MaterialReservation> reservations,
            PreMeasurement preMeasurement
    ) {
        String responseMessage;

        boolean allInStock = reservations.stream()
                .allMatch(r -> r.getStatus().equals(ReservationStatus.IN_STOCK));

        boolean anyPending = reservations.stream()
                .anyMatch(r -> r.getStatus().equals(ReservationStatus.PENDING));

        if (allInStock) {
            preMeasurement.setStatus(ExecutionStatus.AVAILABLE_EXECUTION);
            preMeasurement.setAvailableAt(Instant.now());
            responseMessage =
                    "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. " +
                            "A equipe pode iniciar a execução.";
        } else if (!anyPending) {
            preMeasurement.setStatus(ExecutionStatus.WAITING_COLLECT);
            responseMessage =
                    "Como nenhum material foi reservado em almoxarifado de terceiros, não será necessária aprovação. " +
                            "Mas os materiais estão pendentes de coleta pela equipe.";
        } else {
            preMeasurement.setStatus(ExecutionStatus.WAITING_RESERVE_CONFIRMATION);
            responseMessage =
                    "Como alguns itens foram reservados em almoxarifados de terceiros, será necessária aprovação. " +
                            "Após isso, estes materiais estarão disponíveis para coleta.";
        }

        // persiste mudança de status da pré-medição
        preMeasurementRepository.save(preMeasurement);

        // atualiza o status do gerenciamento da reserva
        namedJdbc.update(
                """
                        UPDATE reservation_management
                        SET status = :status
                        WHERE reservation_management_id = :reservation_management_id
                        """,
                Map.of(
                        "status", ReservationStatus.FINISHED,
                        "reservation_management_id", preMeasurement.getReservationManagementId()
                )
        );

        // marca itens de rua como finalizados
        namedJdbc.update(
                """
                        UPDATE pre_measurement_street_item
                        SET item_status = :status
                        WHERE pre_measurement_id = :preMeasurementID
                        """,
                Map.of(
                        "status", ReservationStatus.FINISHED,
                        "preMeasurementID", preMeasurement.getPreMeasurementId()
                )
        );

        return responseMessage;
    }

    @Transactional
    protected String verifyDirectExecutionReservations(
            List<MaterialReservation> reservations,
            DirectExecution directExecution
    ) {
        String responseMessage;

        boolean allInStock = reservations.stream()
                .allMatch(r -> r.getStatus().equals(ReservationStatus.IN_STOCK));

        boolean anyPending = reservations.stream()
                .anyMatch(r -> r.getStatus().equals(ReservationStatus.PENDING));

        if (allInStock) {
            directExecution.setDirectExecutionStatus(ExecutionStatus.AVAILABLE_EXECUTION);
            directExecution.setAvailableAt(Instant.now());
            responseMessage =
                    "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. " +
                            "A equipe pode iniciar a execução.";
        } else if (!anyPending) {
            directExecution.setDirectExecutionStatus(ExecutionStatus.WAITING_COLLECT);
            responseMessage =
                    "Como nenhum material foi solicitado em almoxarifado de terceiros, não será necessária aprovação. " +
                            "Mas os materiais estão pendentes de coleta pela equipe.";
        } else {
            directExecution.setDirectExecutionStatus(ExecutionStatus.WAITING_RESERVE_CONFIRMATION);
            responseMessage =
                    "Como alguns itens foram solicitados em almoxarifados de terceiros, será necessária aprovação. " +
                            "Após isso, estes materiais estarão disponíveis para coleta.";
        }

        // notificação (método da própria classe/serviço)
//        notify(reservations, String.valueOf(directExecution.getDirectExecutionId()));

        // atualiza o gerenciamento da reserva
        namedJdbc.update(
                """
                        UPDATE reservation_management
                        SET status = :status
                        WHERE reservation_management_id = :reservation_management_id
                        """,
                Map.of(
                        "status", ReservationStatus.FINISHED,
                        "reservation_management_id", directExecution.getReservationManagementId()
                )
        );

        // marca itens da execução direta como finalizados
        namedJdbc.update(
                """
                        UPDATE direct_execution_item
                        SET item_status = :status
                        WHERE direct_execution_id = :directExecutionId
                        """,
                Map.of(
                        "status", ReservationStatus.FINISHED,
                        "directExecutionId", directExecution.getDirectExecutionId()
                )
        );

        // persiste alteração de status da execução
        directExecutionRepository.save(directExecution);

        return responseMessage;
    }

    private void notify(List<MaterialReservation> reservations, String streetIdOrExecutionId) {
        // IDs das reservas
        List<Long> reservationIds = reservations.stream()
                .map(MaterialReservation::getMaterialIdReservation)
                .toList();

        // Status que interessam
        List<String> statuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.APPROVED
        );

        // Telefone da empresa (fallback)
//        String companyPhone = JdbcUtil.getDescription(
//                jdbcTemplate,
//                "company_phone",
//                "company",
//                String.class
//        );
//
//        // Carrega reservas pendentes/aprovadas com dados de depósito e time
//        List<Map<String, Object>> pendingReserves = JdbcUtil.getRawData(
//                namedJdbc,
//                """
//                select mr.status, ms.deposit_id, t.id_team
//                from material_reservation mr
//                inner join material_stock ms on ms.material_id_stock = mr.central_material_stock_id
//                inner join deposit d on d.id_deposit = ms.deposit_id
//                inner join team t on t.id_team = mr.team_id
//                where mr.material_id_reservation in (:material_id_reservations)
//                  and mr.status in (:statuses)
//                """,
//                Map.of(
//                        "material_id_reservations", reservationIds,
//                        "statuses", statuses
//                )
//        );
//
//        // Helpers p/ agrupar por depósito
//        Map<Long, List<Map<String, Object>>> groupedByDepositPending = pendingReserves.stream()
//                .filter(row -> ReservationStatus.PENDING.name().equals(Objects.toString(row.get("status"), null)))
//                .collect(Collectors.groupingBy(row -> toLong(row.get("deposit_id"))));
//
//        Map<Long, List<Map<String, Object>>> groupedByDepositApproved = pendingReserves.stream()
//                .filter(row -> ReservationStatus.APPROVED.name().equals(Objects.toString(row.get("status"), null)))
//                .collect(Collectors.groupingBy(row -> toLong(row.get("deposit_id"))));
//
//        // ===== Notificações para PENDENTES (por depósito) =====
//        for (var entry : groupedByDepositPending.entrySet()) {
//            Long depositId = entry.getKey();
//            List<Map<String, Object>> reserve = entry.getValue();
//            if (reserve.isEmpty()) continue;
//
//            var deposit = depositRepository.findById(depositId != null ? depositId : 0L).orElse(null);
//
//            Long teamId = toLong(reserve.get(0).get("id_team"));
//            var team = teamRepository.findById(teamId != null ? teamId : 0L).orElse(null);
//
//            String bodyMessage = """
//                Por favor, aceite ou negue as solicitações com urgência!
//                """;
//
//            // String teamCode = (team != null) ? team.getTeamCode() : null;
//
//            // if (teamCode != null) {
//            //     notificationService.sendNotificationForTeam(
//            //             teamCode,
//            //             "Existem " + reserve.size() + " materiais pendentes de aprovação no seu almoxarifado (" +
//            //                     (deposit != null ? deposit.getDepositName() : "") + ")",
//            //             bodyMessage,
//            //             "REPLY_RESERVE",
//            //             Instant.now(),
//            //             NotificationType.ALERT,
//            //             streetIdOrExecutionId
//            //     );
//            // }
//        }
//
//        // ===== Notificações para APROVADOS (por depósito) =====
//        for (var entry : groupedByDepositApproved.entrySet()) {
//            Long depositId = entry.getKey();
//            List<Map<String, Object>> reserve = entry.getValue();
//            if (reserve.isEmpty()) continue;
//
//            var deposit = depositRepository.findById(depositId != null ? depositId : 0L).orElse(null);
//
//            Long teamId = toLong(reserve.get(0).get("id_team"));
//            var team = teamRepository.findById(teamId != null ? teamId : 0L).orElse(null);
//
//            // Descobre o estoquista responsável pelo depósito
//            Map<String, Object> stockistRow = JdbcUtil.getSingleRow(
//                    namedJdbc,
//                    """
//                    SELECT user_id_user
//                    FROM stockist
//                    WHERE deposit_id_deposit = :depositId
//                    LIMIT 1
//                    """,
//                    Map.of("depositId", depositId)
//            );
//            String stockistUUID = (stockistRow != null) ? Objects.toString(stockistRow.get("user_id_user"), null) : null;
//
//            // Nome do estoquista (fallbacks)
//            String stockistName = JdbcUtil.getDescription(
//                    jdbcTemplate,
//                    "name || ' ' || last_name",
//                    "app_user",
//                    "user_id",
//                    stockistUUID != null ? UUID.fromString(stockistUUID) : new UUID(0L, 0L),
//                    String.class
//            );
//
//            String depositName = (deposit != null && deposit.getDepositName() != null) ? deposit.getDepositName() : "Desconhecido";
//            String address = (deposit != null && deposit.getDepositAddress() != null) ? deposit.getDepositAddress() : "Endereço não informado";
//            String phone = (deposit != null && deposit.getDepositPhone() != null) ? deposit.getDepositPhone()
//                    : (companyPhone != null ? companyPhone : "Telefone não informado");
//            String responsible = (stockistName != null) ? stockistName : "Responsável não informado";
//
//            int quantity = reserve.size();
//
//            String bodyMessage = """
//                Local: %s
//                Endereço: %s
//                Telefone: %s
//                Responsável: %s
//                """.formatted(depositName, address, phone, responsible);
//
//            // String teamCode = (team != null && team.getTeamCode() != null) ? team.getTeamCode() : "";
//            // notificationService.sendNotificationForTeam(
//            //         teamCode,
//            //         "Existem " + quantity + " materiais pendentes de coleta no almoxarifado " +
//            //                 (deposit != null ? deposit.getDepositName() : ""),
//            //         bodyMessage,
//            //         "",
//            //         Instant.now(),
//            //         NotificationType.ALERT,
//            //         streetIdOrExecutionId
//            // );
//        }
    }

    @Transactional
    public ResponseEntity<?> updateManagement(
            Long reservationManagementId,
            UUID userId,
            Long teamId
    ) {
        var i = installationViewRepository.findInstallationViewByReservationManagementId(reservationManagementId)
                .orElseThrow();
        if (!i.getStatus().equals(ExecutionStatus.WAITING_STOCKIST)) {
            throw new Utils.BusinessException("A Ordem de serviço " + i.getDescription() + " não está mais com o status aguardando estoque");
        }

        reservationManagementRepository.updateStockistId(reservationManagementId, userId);

        if (i.getInstallationType().equals("DIRECT_EXECUTION")) {
            directExecutionRepository.updateTeamId(
                    i.getReservationManagementId(),
                    teamId
            );
        } else {
            preMeasurementInstallationRepository.updateTeamId(
                    i.getReservationManagementId(),
                    teamId
            );
        }

        return ResponseEntity.noContent().build();
    }

    @Transactional
    public ResponseEntity<?> deleteManagement(String currentStatus, Long reservationManagementId) {
        var translatedStatus = switch (currentStatus) {
            case ExecutionStatus.AVAILABLE_EXECUTION -> InstallationStatus.AVAILABLE_EXECUTION.translate("pt");
            case ExecutionStatus.WAITING_STOCKIST -> InstallationStatus.WAITING_STOCKIST.translate("pt");
            default -> throw new Utils.BusinessException(currentStatus + "Não é permitido");
        };

        var i = installationViewRepository.findInstallationViewByReservationManagementId(reservationManagementId)
                .orElseThrow();
        if (!currentStatus.equals(i.getStatus())) {
            throw new Utils.BusinessException("Essa ordem de serviço não está mais com o status " + translatedStatus);
        }

        if (ExecutionStatus.AVAILABLE_EXECUTION.equals(currentStatus)) {
            if ("DIRECT_EXECUTION".equals(i.getInstallationType())) {
                materialReservationRepository.deleteByDirectExecutionId(i.getInstallationId());
            } else {
                materialReservationRepository.deleteByPreMeasurementId(i.getInstallationId());
            }
        }

        if (i.getInstallationType().equals("DIRECT_EXECUTION")) {
            directExecutionRepositoryItem.deleteByDirectExecutionId(i.getInstallationId());
            directExecutionRepository.deleteById(i.getInstallationId());
        } else {
            preMeasurementStreetItemRepository.deleteByPreMeasurementId(i.getInstallationId());
            preMeasurementStreetRepository.deleteByPreMeasurementId(i.getInstallationId());
            preMeasurementRepository.deleteById(i.getInstallationId());
        }

        reservationManagementRepository.deleteById(reservationManagementId);

        return ResponseEntity.noContent().build();
    }
}
