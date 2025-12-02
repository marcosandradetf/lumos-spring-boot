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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class InstallationRegisterService {
    private final MinioService minioService;
    private final PreMeasurementInstallationRepository preMeasurementInstallationRepository;
    private final MaterialStockRepository materialStockRepository;
    private final PreMeasurementExecutorRepository preMeasurementExecutorRepository;

    public InstallationRegisterService(
            MinioService minioService,
            PreMeasurementInstallationRepository preMeasurementInstallationRepository,
            MaterialStockRepository materialStockRepository,
            PreMeasurementExecutorRepository preMeasurementExecutorRepository
    ) {
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

        if (!Objects.equals(installation.status(), ExecutionStatus.AVAILABLE_EXECUTION)) {
            return ResponseEntity.noContent().build();
        }

        for (InstallationItemRequest r : installationReq.getItems()) {
            String materialName = r.getMaterialName().toLowerCase(Locale.ROOT);
            String hasService = null;

            if (materialName.contains("led")) {
                hasService = "led";
            } else if (materialName.contains("braço")) {
                hasService = "braço";
            }

            if (hasService != null) {
                preMeasurementInstallationRepository.updateExecutedQuantity(
                        r.getContractItemId(),
                        hasService,
                        installation.preMeasurementId(),
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

        String fileUri = null;
        if (photo != null) {
            String description = installation.description();
            var folder = new StringBuilder()
                    .append("photos");

            if (description != null) {
                folder.append("/")
                        .append(description.replaceAll("\\s+", "_"));
            }

            fileUri = minioService.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder.toString(), "execution");
        }
        minioService.deleteFiles(Utils.INSTANCE.getCurrentBucket(), Set.of(installation.preMeasurementPhotoUri()));

        preMeasurementInstallationRepository.saveInstallationStreetPhotoUri(
                fileUri, ExecutionStatus.FINISHED, installationReq.getStreetId()
        );

        return ResponseEntity.noContent().build();
    }

    @Transactional
    public ResponseEntity<?> saveInstallation(MultipartFile photo, InstallationRequest installationReq) {
        if (installationReq == null) {
            throw new Utils.BusinessException("payload vazio.");
        }

        var installation = preMeasurementInstallationRepository.getInstallationByDeviceInstallationId(installationReq.getInstallationId());
        Long installationId = installation.preMeasurementId();
        String description = installation.description();
        String status = installation.status();

        if (Objects.equals(status, ExecutionStatus.FINISHED)) {
            return ResponseEntity.noContent().build();
        }

        String fileUri = null;
        if (photo != null) {
            var folder = new StringBuilder()
                    .append("photos");

            if (description != null) {
                folder.append("/")
                        .append(description.replaceAll("\\s+", "_"));
            }

            if (installationReq.getResponsible() != null) {
                folder.append("/")
                        .append(installationReq.getResponsible().replaceAll("\\s+", "_"));
            }

            fileUri = minioService.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder.toString(), "installation");
        }

        preMeasurementInstallationRepository.saveInstallationSignPhotoUri(
                fileUri,
                installationReq.getSignDate(),
                installationReq.getResponsible(),
                ExecutionStatus.FINISHED,
                installationId
        );

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
