package com.lumos.lumosspring.premeasurement.service.installation;

import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationItemRequest;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationRequest;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationStreetRequest;
import com.lumos.lumosspring.premeasurement.model.PreMeasurementExecutor;
import com.lumos.lumosspring.premeasurement.repository.installation.PreMeasurementExecutorRepository;
import com.lumos.lumosspring.premeasurement.repository.installation.PreMeasurementInstallationRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRepository;
import com.lumos.lumosspring.util.ExecutionStatus;
import com.lumos.lumosspring.util.Utils;
import kotlin.text.Regex;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class InstallationRegisterService {
    private final NamedParameterJdbcTemplate namedJdbc;
    private final MinioService minioService;
    private final PreMeasurementInstallationRepository preMeasurementInstallationRepository;
    private final MaterialStockRepository materialStockRepository;
    private final PreMeasurementExecutorRepository preMeasurementExecutorRepository;

    public InstallationRegisterService(NamedParameterJdbcTemplate namedJdbc, MinioService minioService, PreMeasurementInstallationRepository preMeasurementInstallationRepository, MaterialStockRepository materialStockRepository, PreMeasurementExecutorRepository preMeasurementExecutorRepository) {
        this.namedJdbc = namedJdbc;
        this.minioService = minioService;
        this.preMeasurementInstallationRepository = preMeasurementInstallationRepository;
        this.materialStockRepository = materialStockRepository;
        this.preMeasurementExecutorRepository = preMeasurementExecutorRepository;
    }

    @Transactional
    public ResponseEntity<?> saveStreetInstallation(MultipartFile photo, InstallationStreetRequest installationReq) {
        if (installationReq == null) {
            throw new Utils.BusinessException("payload vazio.");
        }

        var installation = preMeasurementInstallationRepository.getInstallationByDeviceStreetId(installationReq.getStreetId());

        if (installation == null) {
            String message = new StringBuilder()
                    .append("Instalação com ID ")
                    .append(installationReq.getStreetId())
                    .append(" não encontrada")
                    .toString();
            throw new Utils.BusinessException(message);
        }

        if (!Objects.equals(installation.get("status").toString(), ExecutionStatus.AVAILABLE_EXECUTION)) {
            return ResponseEntity.noContent().build();
        }

        Long preMeasurementID = ((Number) installation.get("pre_measurement_id")).longValue();

        String sql;
        for (InstallationItemRequest r : installationReq.getItems()) {
            String materialName = r.getMaterialName().toLowerCase(Locale.ROOT);
            String hasService;

            if (materialName.contains("led")) {
                hasService = "led";
            } else if (materialName.contains("braço")) {
                hasService = "braço";
            } else {
                hasService = null;
            }


            if (hasService != null) {

                preMeasurementInstallationRepository.updateExecutedQuantity(
                        r.getContractItemId(),
                        hasService,
                        preMeasurementID,
                        r.getQuantityExecuted()
                );


            } else {
                preMeasurementInstallationRepository.updateExecutedQuantity(
                        r.getContractItemId(),
                        r.getQuantityExecuted()
                );
            }

            materialStockRepository.debitStock(
                    r.getQuantityExecuted(),
                    r.getTruckMaterialStockId()
            );

        }

        if (photo != null) {
            String city = ((String) installation.get("city"));
            var folder = new StringBuilder()
                    .append("photos");

            if (city != null) {
                folder.append("/")
                        .append(city);
            }

            String fileUri = minioService.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder.toString(), "execution");

            preMeasurementInstallationRepository.saveInstallationStreetPhotoUri(
                    fileUri, ExecutionStatus.FINISHED, installationReq.getStreetId()
            );
        } else {
            preMeasurementInstallationRepository.saveInstallationStreetPhotoUri(
                    null, ExecutionStatus.FINISHED, installationReq.getStreetId()
            );
        }

        return ResponseEntity.noContent().build();
    }

    @Transactional
    public ResponseEntity<?> saveInstallation(MultipartFile photo, InstallationRequest installationReq) {
        if (installationReq == null) {
            throw new Utils.BusinessException("payload vazio.");
        }

        Map<String, Object> installation = preMeasurementInstallationRepository.getInstallationByDeviceInstallationId(installationReq.getInstallationId());
        Long installationId = ((Long) installation.get("pre_measurement_id"));
        String city = ((String) installation.get("city"));
        String status = ((String) installation.get("status"));

        if (Objects.equals(status, ExecutionStatus.FINISHED)) {
            return ResponseEntity.noContent().build();
        }

        if (photo != null) {
            var folder = new StringBuilder()
                    .append("photos");

            if (city != null) {
                folder.append("/")
                        .append(city);
            }

            if (installationReq.getResponsible() != null) {
                folder.append("/")
                        .append(installationReq.getResponsible().replaceAll("\\s+", "_"));
            }

            String fileUri = minioService.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder.toString(), "installation");

            preMeasurementInstallationRepository.saveInstallationSignPhotoUri(
                    fileUri, installationReq.getSignDate(),
                    installationReq.getResponsible(),
                    ExecutionStatus.FINISHED, installationId
            );
        } else {
            preMeasurementInstallationRepository.saveInstallationSignPhotoUri(
                    null, null,
                    null,
                    ExecutionStatus.FINISHED, installationId
            );
        }

        var executors = installationReq.getOperationalUsers()
                .stream()
                .map(userId ->
                        new PreMeasurementExecutor(installationId, userId, true)
                ).toList();
        if (!executors.isEmpty()) {
            preMeasurementExecutorRepository.saveAll(executors);
        }

        return ResponseEntity.noContent().build();
    }


}
