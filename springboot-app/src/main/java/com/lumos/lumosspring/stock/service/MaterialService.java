package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.entities.*;
import com.lumos.lumosspring.stock.repository.*;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.dto.stock.MaterialRequest;
import com.lumos.lumosspring.dto.stock.MaterialResponse;
import com.lumos.lumosspring.system.entities.Log;
import com.lumos.lumosspring.system.repository.LogRepository;
import com.lumos.lumosspring.util.Util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialService {
    private final MaterialStockRepository materialStockRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final TypeRepository tipoRepository;
    private final DepositRepository depositRepository;
    private final CompanyRepository companyRepository;
    private final TypeRepository typeRepository;
    private final MaterialRepository materialRepository;
    private final Util util;
    private final MaterialStockJdbcRepository materialStockJdbcRepository;

    public MaterialService(MaterialStockRepository materialStockRepository, UserRepository userRepository, LogRepository logRepository, TypeRepository tipoRepository, DepositRepository depositRepository, CompanyRepository companyRepository, TypeRepository typeRepository, MaterialRepository materialRepository, Util util, MaterialStockJdbcRepository materialStockJdbcRepository) {

        this.materialStockRepository = materialStockRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.tipoRepository = tipoRepository;
        this.depositRepository = depositRepository;
        this.companyRepository = companyRepository;
        this.typeRepository = typeRepository;
        this.materialRepository = materialRepository;
        this.util = util;
        this.materialStockJdbcRepository = materialStockJdbcRepository;
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

//    @Transactional
//    public ResponseEntity<?> save(MaterialRequest material) {
//        // Validação do material
//        ResponseEntity<String> validationError = validateMaterialRequest(material);
//        if (validationError != null) {
//            return validationError;
//        }
//
//        return ResponseEntity.ok(convertToMaterialResponse(convertToMaterial(material)));
//    }

    private ResponseEntity<String> validateMaterialRequest(MaterialRequest material) {
        if (materialStockRepository.existsMaterial(
                material.materialName(), material.deposit(), material.materialBrand())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Este material já existe no almoxarifado informado.");
        }

        if (!tipoRepository.existsById(material.materialType())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Tipo de material não encontrado.");
        }

        if (!material.allDeposits() && !depositRepository.existsById(material.deposit())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Almoxarifado não encontrado.");
        }

        if (!companyRepository.existsById(material.company())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Empresa não encontrada.");
        }
        return null; // Indica que não houve erros
    }

    private List<MaterialStock> convertToMaterial(MaterialRequest material) {
        List<MaterialStock> materialList = new ArrayList<>();

        MaterialType materialType = tipoRepository.findById(material.materialType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo informado não encontrado."));

        Company company = companyRepository.findById(material.company())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));

        if (material.allDeposits()) {
            Iterable<Deposit> deposits = depositRepository.findAll();
            for (Deposit deposit : deposits) {
                MaterialStock materialStock = new MaterialStock();
                materialStock.setMaterialId(createMaterial(material, materialType).getIdMaterial());
                materialStock.setCompanyId(company.getIdCompany());
                materialStock.setDepositId(deposit.getIdDeposit());
                materialStock.setBuyUnit(material.buyUnit());
                materialStock.setRequestUnit(material.requestUnit());
                materialList.add(materialStock);
            }
        } else {
            MaterialStock materialStock = new MaterialStock();
            materialStock.setMaterialId(createMaterial(material, materialType).getIdMaterial());
            materialStock.setCompanyId(company.getIdCompany());
            Deposit deposit = depositRepository.findById(material.deposit())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Almoxarifado não encontrado."));
            materialStock.setDepositId(deposit.getIdDeposit());
            materialStock.setBuyUnit(material.buyUnit());
            materialStock.setRequestUnit(material.requestUnit());
            materialList.add(materialStock);
        }

        // Salvar todos os materiais de uma vez
        materialStockRepository.saveAll(materialList);

        return materialList;
    }

    private Material createMaterial(MaterialRequest material, MaterialType materialType) {
        Material newMaterial = new Material();
        newMaterial.setMaterialName(material.materialName());
        newMaterial.setMaterialBrand(material.materialBrand());
        newMaterial.setMaterialPower(material.materialPower());
        newMaterial.setMaterialAmps(material.materialAmps());  // Corrigido
        newMaterial.setMaterialLength(material.materialLength());  // Corrigido
        newMaterial.setIdMaterialType(materialType.getIdType());
        return newMaterial;
    }


//    private List<MaterialResponse> convertToMaterialResponse(List<MaterialStock> materialList) {
//        return materialList.stream()
//                .map(ms -> new MaterialResponse(
//                        ms.getMaterialIdStock(),
//                        ms.getMaterial().getMaterialName(),
//                        ms.getMaterial().getMaterialBrand(),
//                        ms.getMaterial().getMaterialPower(),
//                        ms.getMaterial().getMaterialAmps(),
//                        ms.getMaterial().getMaterialLength(),
//                        ms.getBuyUnit(),
//                        ms.getRequestUnit(),
//                        ms.getStockQuantity(),
//                        ms.isInactive(),
//                        ms.getMaterial().getMaterialType() != null ? ms.getMaterial().getMaterialType().getTypeName() : null,
//                        ms.getMaterial().getMaterialType() != null && ms.getMaterial().getMaterialType().getGroup() != null
//                                ? ms.getMaterial().getMaterialType().getGroup().getGroupName()
//                                : null,
//                        ms.getDeposit() != null ? ms.getDeposit().getDepositName() : null,
//                        ms.getCompany() != null ? ms.getCompany().getSocialReason() : null
//                ))
//                .toList();
//    }


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

//    @Transactional
//    public ResponseEntity<?> update(MaterialRequest material, Long materialId, UUID idUsuario) {
//        var user = userRepository.findById(idUsuario);
//        var existingMaterial = materialStockRepository.findById(materialId);
//
//        // Verifica se o material existe
//        if (existingMaterial.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body("Material não encontrado.");
//        }
//
//        // Verifica se o usuário existe
//        if (user.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body("Usuário não encontrado.");
//        }
//
//        // Atualiza os campos necessários do material existente
//        MaterialStock materialToUpdate = existingMaterial.get();
//        materialToUpdate.getMaterial().setMaterialName(material.materialName());
//        materialToUpdate.getMaterial().setMaterialBrand(material.materialBrand());
//
//        materialToUpdate.getMaterial().setMaterialPower(material.materialPower());
//        materialToUpdate.getMaterial().setMaterialAmps(material.materialAmps());
//        materialToUpdate.getMaterial().setMaterialLength(material.materialLength());
//
//        materialToUpdate.setBuyUnit(material.buyUnit());
//        materialToUpdate.setRequestUnit(material.requestUnit());
//        materialToUpdate.setInactive(material.inactive());
//        materialToUpdate.getMaterial().setMaterialType(typeRepository.findById(material.materialType()).orElse(null));
//
//        materialStockRepository.save(materialToUpdate);
//
//        // Cria e salva o log de atualização
//        var log = new Log();
//        String logMessage = String.format("Usuário %s atualizou material %d com sucesso.",
//                user.get().getUsername(),
//                materialId);
//
//        log.setMessage(logMessage);
//        log.setUser(user.get());
//        log.setCategory("Estoque");
//        log.setType("Atualização");
//        logRepository.save(log);
//
//        return ResponseEntity.ok(convertToMaterialResponse(List.of(materialToUpdate)));
//    }


    public ResponseEntity<?> findAllForImportPreMeasurement() {
        List<Material> materials = materialRepository.findAllForImportPreMeasurement(); // ou equivalente
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


}
