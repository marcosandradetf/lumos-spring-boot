package com.lumos.lumosspring.premeasurement.service.installation;

import com.lumos.lumosspring.contract.repository.ContractItemDependencyRepository;
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository;
import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationItemRequest;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationRequest;
import com.lumos.lumosspring.premeasurement.dto.installation.InstallationStreetRequest;
import com.lumos.lumosspring.premeasurement.model.PreMeasurementExecutor;
import com.lumos.lumosspring.premeasurement.repository.installation.PreMeasurementExecutorRepository;
import com.lumos.lumosspring.premeasurement.repository.installation.PreMeasurementInstallationRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.util.ExecutionStatus;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

@Service
public class InstallationRegisterService {
    private final MinioService minioService;
    private final PreMeasurementInstallationRepository preMeasurementInstallationRepository;
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final PreMeasurementExecutorRepository preMeasurementExecutorRepository;
    private final ContractItemDependencyRepository contractItemDependencyRepository;
    private final ContractItemsQuantitativeRepository contractItemsQuantitativeRepository;

    public InstallationRegisterService(
            MinioService minioService,
            PreMeasurementInstallationRepository preMeasurementInstallationRepository,
            MaterialStockRegisterRepository materialStockRegisterRepository,
            PreMeasurementExecutorRepository preMeasurementExecutorRepository,
            ContractItemDependencyRepository contractItemDependencyRepository, ContractItemsQuantitativeRepository contractItemsQuantitativeRepository) {
        this.minioService = minioService;
        this.preMeasurementInstallationRepository = preMeasurementInstallationRepository;
        this.materialStockRegisterRepository = materialStockRegisterRepository;
        this.preMeasurementExecutorRepository = preMeasurementExecutorRepository;
        this.contractItemDependencyRepository = contractItemDependencyRepository;
        this.contractItemsQuantitativeRepository = contractItemsQuantitativeRepository;
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
            preMeasurementInstallationRepository.updateInstallationItem(r.getContractItemId(), r.getQuantityExecuted(), installation.preMeasurementStreetId());
            contractItemsQuantitativeRepository.updateBalance(
                    r.getContractItemId(),
                    r.getQuantityExecuted()
            );
            saveLinkedItems(r.getContractItemId(), r.getQuantityExecuted(), installation.preMeasurementStreetId());

            materialStockRegisterRepository.debitStock(
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

        preMeasurementInstallationRepository.finishInstallationStreet(
                fileUri, ExecutionStatus.FINISHED, installationReq.getStreetId(), installationReq.getCurrentSupply(),
                installationReq.getLastPower(), installationReq.getLatitude(), installationReq.getLongitude()
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


    private void saveLinkedItems(Long contractItemId, BigDecimal quantityExecuted, Long preMeasurementStreetId) {
        var itemDependency = contractItemDependencyRepository.getAllPreMeasurementItemsById(contractItemId, preMeasurementStreetId);

        itemDependency.forEach(dependency -> {
            preMeasurementInstallationRepository.updateInstallationItem(
                    dependency.getContractItemId(),
                    quantityExecuted.multiply(dependency.getFactor()),
                    preMeasurementStreetId
            );

            contractItemsQuantitativeRepository.updateBalance(
                    dependency.getContractItemId(),
                    quantityExecuted.multiply(dependency.getFactor())
            );
        });
    }

}
