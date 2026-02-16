package com.lumos.lumosspring.installation.service.premeasurement;

import com.lumos.lumosspring.contract.repository.ContractItemDependencyRepository;
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository;
import com.lumos.lumosspring.installation.dto.premeasurement.InstallationItemRequest;
import com.lumos.lumosspring.installation.dto.premeasurement.InstallationRequest;
import com.lumos.lumosspring.installation.dto.premeasurement.InstallationStreetRequest;
import com.lumos.lumosspring.installation.model.premeasurement.PreMeasurementExecutor;
import com.lumos.lumosspring.installation.repository.premeasurement.PreMeasurementExecutorRepository;
import com.lumos.lumosspring.installation.repository.premeasurement.PreMeasurementInstallationRepository;
import com.lumos.lumosspring.s3.service.S3Service;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.util.ExecutionStatus;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RegisterService {
    private final S3Service s3Service;
    private final PreMeasurementInstallationRepository preMeasurementInstallationRepository;
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final PreMeasurementExecutorRepository preMeasurementExecutorRepository;
    private final ContractItemDependencyRepository contractItemDependencyRepository;
    private final ContractItemsQuantitativeRepository contractItemsQuantitativeRepository;

    public RegisterService(
            S3Service s3Service,
            PreMeasurementInstallationRepository preMeasurementInstallationRepository,
            MaterialStockRegisterRepository materialStockRegisterRepository,
            PreMeasurementExecutorRepository preMeasurementExecutorRepository,
            ContractItemDependencyRepository contractItemDependencyRepository, ContractItemsQuantitativeRepository contractItemsQuantitativeRepository) {
        this.s3Service = s3Service;
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

        if(!List.of(ExecutionStatus.AVAILABLE_EXECUTION, ExecutionStatus.IN_PROGRESS).contains(installation.status())) {
            return ResponseEntity.noContent().build();
        }

        for (InstallationItemRequest r : installationReq.getItems()) {
            preMeasurementInstallationRepository.updateInstallationItem(r.getContractItemId(), r.getQuantityExecuted(), installation.preMeasurementStreetId());
            contractItemsQuantitativeRepository.updateBalance(
                    r.getContractItemId(),
                    r.getQuantityExecuted()
            );
            saveLinkedItems(r.getContractItemId(), r.getQuantityExecuted(), installation.preMeasurementStreetId());

            if(r.getTruckStockControl()) {
                materialStockRegisterRepository.debitStock(
                        r.getQuantityExecuted(),
                        r.getTruckMaterialStockId()
                );
            }
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

            fileUri = s3Service.uploadFile(photo, Utils.INSTANCE.getCurrentBucket(), folder.toString(), "execution", Utils.getCurrentTenantId());
        }
        s3Service.deleteFiles(Utils.INSTANCE.getCurrentBucket(), Set.of(installation.preMeasurementPhotoUri()));

        preMeasurementInstallationRepository.finishInstallationStreet(
                fileUri, ExecutionStatus.FINISHED, installationReq.getStreetId(), installationReq.getCurrentSupply(),
                installationReq.getLastPower(), installationReq.getLatitude(), installationReq.getLongitude(), installationReq.getFinishedAt()
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

            fileUri = s3Service.uploadFile(photo, Utils.getCurrentBucket(), folder.toString(), "installation", Utils.getCurrentTenantId());
        }

        preMeasurementInstallationRepository.saveInstallationSignPhotoUri(
                fileUri,
                installationReq.getSignDate(),
                installationReq.getResponsible(),
                ExecutionStatus.FINISHED,
                installationId,
                Objects.requireNonNullElse(installationReq.getSignDate(), Instant.now()),
                installationReq.getStartedAt()
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
