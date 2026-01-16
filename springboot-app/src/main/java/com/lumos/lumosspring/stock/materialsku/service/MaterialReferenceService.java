package com.lumos.lumosspring.stock.materialsku.service;

import com.lumos.lumosspring.stock.deposit.repository.DepositRepository;
import com.lumos.lumosspring.stock.materialsku.model.Material;
import com.lumos.lumosspring.stock.materialsku.model.MaterialContractReferenceItem;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialContractReferenceItemRepository;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialstock.model.MaterialStock;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.stock.materialsku.dto.MaterialRequest;
import com.lumos.lumosspring.system.entities.Log;
import com.lumos.lumosspring.system.repository.LogRepository;

import com.lumos.lumosspring.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialReferenceService {
    private final MaterialStockRegisterRepository materialStockRegisterRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final MaterialReferenceRepository materialReferenceRepository;
    private final MaterialContractReferenceItemRepository materialContractReferenceItemRepository;
    private final DepositRepository depositRepository;

    public MaterialReferenceService(MaterialStockRegisterRepository materialStockRegisterRepository,
                                    UserRepository userRepository,
                                    LogRepository logRepository,
                                    MaterialReferenceRepository materialReferenceRepository,
                                    MaterialContractReferenceItemRepository materialContractReferenceItemRepository, DepositRepository depositRepository) {

        this.materialStockRegisterRepository = materialStockRegisterRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;

        this.materialReferenceRepository = materialReferenceRepository;
        this.materialContractReferenceItemRepository = materialContractReferenceItemRepository;
        this.depositRepository = depositRepository;
    }

    @Transactional
    public ResponseEntity<?> deleteById(Long idMaterial, UUID userId) {
        var user = userRepository.findById(userId);
        var material = materialStockRegisterRepository.findById(idMaterial);
        var log = new Log();

        if (material.isPresent() && user.isPresent()) {
            if (material.get().isInactive()) {
                materialStockRegisterRepository.delete(material.get());
                String logMessage = String.format("Usuário %s excluiu material %d com sucesso.",
                        user.get().getUsername(),
                        material.get().getMaterialId());

                log.setMessage(logMessage);
                log.setIdUser(user.get().getUserId());
                log.setCategory("Estoque");
                log.setType("Sucess");
                logRepository.save(log);

            } else {
                String errorMessage = "Não é possível excluir um material ativo.";
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMessage);
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> findAllForImportPreMeasurement() {
        List<Material> materials = materialReferenceRepository.findAllForImportPreMeasurement(); // ou equivalente
        record MaterialDTOImport(long idMaterial, String materialName, String materialBrand, String materialPower,
                                 String materialAmps, String materialLength) {
        }
        List<MaterialDTOImport> materialsDTO = new ArrayList<>();

        for (Material m : materials) {
            materialsDTO.add(new MaterialDTOImport(
                    m.getIdMaterial(),
                    m.getNameForImport(),
                    m.getMaterialBrand(),
                    m.getMaterialPower(),
                    m.getMaterialAmps(),
                    m.getMaterialLength()
            ));
        }

        return ResponseEntity.ok(materialsDTO);
    }

    @Transactional
    public ResponseEntity<?> create(MaterialRequest material) {
        // Cria e salva o log de atualização
        var user = userRepository.findByUserId(Utils.INSTANCE.getCurrentUserId()).orElseThrow();
        Long baseMaterialId = materialReferenceRepository.findBaseMaterialId(material.materialBaseName(), Utils.INSTANCE.getCurrentTenantId());
        var log = new Log();
        String logMessage;

        if (material.materialId() == null) {
            if (baseMaterialId == null) {
                var baseMaterial = new Material(
                        material.materialBaseName(),
                        material.materialType(),
                        material.materialSubtype(),
                        material.truckStockControl()
                );
                baseMaterial = materialReferenceRepository.save(baseMaterial);
                baseMaterialId = baseMaterial.getIdMaterial();
            }

            var materialSku = new Material(
                    baseMaterialId,
                    material.materialName(),
                    material.materialType(),
                    material.materialSubtype(),
                    material.materialFunction(),
                    material.materialModel(),
                    material.materialBrand(),
                    material.materialAmps(),
                    material.materialLength(),
                    material.materialWidth(),
                    material.materialPower(),
                    material.materialGauge(),
                    material.materialWeight(),
                    material.barcode(),
                    material.buyUnit(),
                    material.requestUnit(),
                    material.truckStockControl()
            );
            materialSku = materialReferenceRepository.save(materialSku);
            Long materialId = materialSku.getIdMaterial();

            material.contractItems().forEach(itemId -> {
                materialContractReferenceItemRepository.save(
                        new MaterialContractReferenceItem(
                                materialId,
                                itemId,
                                true
                        )
                );
            });

            var depositsIds = depositRepository.findAllDepositIds();
            for (Long depositId : depositsIds) {
                var materialStock = new MaterialStock(
                        materialId,
                        depositId,
                        material.buyUnit(),
                        material.requestUnit(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        false
                );
                materialStockRegisterRepository.save(materialStock);
            }

            logMessage = String.format("Usuário %s criou o material %d - %s com sucesso.",
                    user.getUsername(),
                    materialId,
                    material.materialName()
            );
            log.setType("Criação");
        } else {
            Long materialId = material.materialId();
            var materialSku = materialReferenceRepository.findById(materialId).orElseThrow();
            if (baseMaterialId == null) {
                var baseMaterial = new Material(
                        material.materialBaseName(),
                        material.materialType(),
                        material.materialSubtype(),
                        material.truckStockControl()
                );
                baseMaterial = materialReferenceRepository.save(baseMaterial);
                baseMaterialId = baseMaterial.getIdMaterial();
            }

            materialSku.update(
                    baseMaterialId,
                    material.materialName(),
                    material.materialType(),
                    material.materialSubtype(),
                    material.materialFunction(),
                    material.materialModel(),
                    material.materialBrand(),
                    material.materialAmps(),
                    material.materialLength(),
                    material.materialWidth(),
                    material.materialPower(),
                    material.materialGauge(),
                    material.materialWeight(),
                    material.barcode(),
                    material.inactive(),
                    material.buyUnit(),
                    material.requestUnit(),
                    material.truckStockControl()
            );
            materialReferenceRepository.save(materialSku);

            materialContractReferenceItemRepository.deleteByMaterialId(materialId);
            material.contractItems().forEach(itemId -> {
                materialContractReferenceItemRepository.save(
                        new MaterialContractReferenceItem(
                                materialId,
                                itemId,
                                true
                        )
                );
            });

            logMessage = String.format("Usuário %s atualizou o material %d - %s com sucesso.",
                    user.getUsername(),
                    material.materialId(),
                    material.materialName()
            );
            log.setType("Atualização");
        }

        log.setMessage(logMessage);
        log.setIdUser(user.getUserId());
        log.setCategory("Estoque");
        logRepository.save(log);

        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> findByBarcode(String barcode) {
        var material = materialReferenceRepository.findByBarcodeAndTenantId(barcode, Utils.INSTANCE.getCurrentTenantId()).orElse(null);
        if (material == null) {
            material = materialReferenceRepository.findFirstByBarcode(barcode)
                    .orElseThrow(() -> new Utils.BusinessException("Material não encontrado"));
            material.setIdMaterial(null);
        }

        List<Long> items = new ArrayList<>();
        if (material.getIdMaterial() != null) {
            items = materialContractReferenceItemRepository.findAllByMaterialId(material.getIdMaterial());
        }

        var response = new MaterialRequest(
                material.getIdMaterial(),
                null,
                material.getMaterialName(),
                material.getIdMaterialType(),
                material.getSubtypeId(),
                material.getMaterialFunction(),
                material.getMaterialModel(),
                material.getMaterialBrand(),
                material.getMaterialAmps(),
                material.getMaterialLength(),
                material.getMaterialWidth(),
                material.getMaterialPower(),
                material.getMaterialGauge(),
                material.getMaterialWeight(),
                material.getBarcode(),
                material.getInactive(),
                material.getBuyUnit(),
                material.getRequestUnit(),
                material.getTruckStockControl(),
                items
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> findById(Long materialId) {
        var material = materialReferenceRepository.findById(materialId)
                .orElseThrow(() -> new Utils.BusinessException("Material não encontrado"));

        var items = materialContractReferenceItemRepository.findAllByMaterialId(material.getIdMaterial());

        var response = new MaterialRequest(
                material.getIdMaterial(),
                null,
                material.getMaterialName(),
                material.getIdMaterialType(),
                material.getSubtypeId(),
                material.getMaterialFunction(),
                material.getMaterialModel(),
                material.getMaterialBrand(),
                material.getMaterialAmps(),
                material.getMaterialLength(),
                material.getMaterialWidth(),
                material.getMaterialPower(),
                material.getMaterialGauge(),
                material.getMaterialWeight(),
                material.getBarcode(),
                material.getInactive(),
                material.getBuyUnit(),
                material.getRequestUnit(),
                material.getTruckStockControl(),
                items
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getCatalogue() {
        var materials = materialReferenceRepository.getCatalogue(Utils.INSTANCE.getCurrentTenantId());
        return ResponseEntity.ok(materials);
    }


}
