package com.lumos.lumosspring.stock.materialsku.service;

import com.lumos.lumosspring.company.repository.CompanyRepository;
import com.lumos.lumosspring.stock.materialsku.model.Material;
import com.lumos.lumosspring.stock.materialsku.model.MaterialContractReferenceItem;
import com.lumos.lumosspring.stock.deposit.repository.DepositRepository;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialContractReferenceItemRepository;
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialsku.repository.TypeRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockJdbcRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRepository;
import com.lumos.lumosspring.stock.materialstock.repository.PagedResponse;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.stock.materialsku.dto.MaterialRequest;
import com.lumos.lumosspring.stock.materialsku.dto.MaterialResponse;
import com.lumos.lumosspring.system.entities.Log;
import com.lumos.lumosspring.system.repository.LogRepository;

import com.lumos.lumosspring.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialReferenceService {
    private final MaterialStockRepository materialStockRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final TypeRepository typeRepository;
    private final DepositRepository depositRepository;
    private final CompanyRepository companyRepository;
    private final MaterialReferenceRepository materialReferenceRepository;
    private final MaterialStockJdbcRepository materialStockJdbcRepository;
    private final MaterialContractReferenceItemRepository materialContractReferenceItemRepository;

    public MaterialReferenceService(MaterialStockRepository materialStockRepository, UserRepository userRepository, LogRepository logRepository, TypeRepository tipoRepository, DepositRepository depositRepository, CompanyRepository companyRepository, MaterialReferenceRepository materialReferenceRepository, MaterialStockJdbcRepository materialStockJdbcRepository, MaterialContractReferenceItemRepository materialContractReferenceItemRepository) {

        this.materialStockRepository = materialStockRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.typeRepository = tipoRepository;
        this.depositRepository = depositRepository;
        this.companyRepository = companyRepository;
        this.materialReferenceRepository = materialReferenceRepository;
        this.materialStockJdbcRepository = materialStockJdbcRepository;
        this.materialContractReferenceItemRepository = materialContractReferenceItemRepository;
    }

    public ResponseEntity<PagedResponse<MaterialResponse>> findAll(Integer page, Integer size, Long depositId) {
        PagedResponse<MaterialResponse> materials;
        if (depositId <= 0)
            materials = materialStockJdbcRepository.findAllMaterialsStock(page, size);
        else
            materials = materialStockJdbcRepository.findAllMaterialsStockByDeposit(page, size, depositId);

        return ResponseEntity.ok(materials);
    }

    public ResponseEntity<PagedResponse<MaterialResponse>> searchMaterialStock(String name, Integer page, Integer size, Long depositId) {
        PagedResponse<MaterialResponse> materials;

        if (depositId <= 0)
            materials = materialStockJdbcRepository.searchMaterial(name.toLowerCase(), page, size);
        else
            materials = materialStockJdbcRepository.searchMaterialWithDeposit(name.toLowerCase(), depositId, page, size);

        return ResponseEntity.ok(materials);  // Retorna os materiais no formato de resposta
    }



    @Transactional
    public ResponseEntity<?> deleteById(Long idMaterial, UUID userId) {
        var user = userRepository.findById(userId);
        var material = materialStockRepository.findById(idMaterial);
        var log = new Log();

        if (material.isPresent() && user.isPresent()) {
            if (material.get().isInactive()) {
                materialStockRepository.delete(material.get());
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


    public ResponseEntity<?> create(MaterialRequest material) {
        // Cria e salva o log de atualização
        var user = userRepository.findByUserId(Utils.INSTANCE.getCurrentUserId()).orElseThrow();
        var log = new Log();
        String logMessage;

        if(material.materialId() == null) {
            Long baseMaterialId = materialReferenceRepository.findBaseMaterialId(material.materialBaseName());
            if(baseMaterialId == null) {
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

            logMessage = String.format("Usuário %s criou o material %d - %s com sucesso.",
                    user.getUsername(),
                    materialId,
                    material.materialName()
            );
            log.setType("Criação");
        } else {
            Long materialId = material.materialId();
            var materialSku = materialReferenceRepository.findById(materialId).orElseThrow();
            Long baseMaterialId = materialReferenceRepository.findBaseMaterialId(material.materialBaseName());
            if(baseMaterialId == null) {
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
        var material = materialReferenceRepository.findAllByBarcodeAndTenantId(barcode, Utils.INSTANCE.getCurrentTenantId());
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

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
}
