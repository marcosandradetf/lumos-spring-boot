package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.mobile.MaterialDTOMob;
import com.lumos.lumosspring.stock.entities.Company;
import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.stock.controller.dto.MaterialRequest;
import com.lumos.lumosspring.stock.controller.dto.MaterialResponse;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.DepositRepository;
import com.lumos.lumosspring.stock.repository.CompanyRepository;
import com.lumos.lumosspring.stock.repository.TypeRepository;
import com.lumos.lumosspring.system.entities.Log;
import com.lumos.lumosspring.system.repository.LogRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final TypeRepository tipoRepository;
    private final DepositRepository depositRepository;
    private final CompanyRepository companyRepository;
    private final TypeRepository typeRepository;

    public MaterialService(MaterialRepository materialRepository, UserRepository userRepository, LogRepository logRepository, TypeRepository tipoRepository, DepositRepository depositRepository, CompanyRepository companyRepository, TypeRepository typeRepository) {
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.tipoRepository = tipoRepository;
        this.depositRepository = depositRepository;
        this.companyRepository = companyRepository;
        this.typeRepository = typeRepository;
    }

    public Page<Material> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return materialRepository.findAllOrderByIdMaterial(pageable);
    }

    @Transactional
    public ResponseEntity<?> save(MaterialRequest material) {
        // Validação do material
        ResponseEntity<String> validationError = validateMaterialRequest(material);
        if (validationError != null) {
            return validationError;
        }

        return ResponseEntity.ok(convertToMaterialResponse(convertToMaterial(material)));
    }

    private ResponseEntity<String> validateMaterialRequest(MaterialRequest material) {
        if (materialRepository.existsMaterial(
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

    private List<Material> convertToMaterial(MaterialRequest material) {
        List<Material> materialList = new ArrayList<>();

        Type type = tipoRepository.findById(material.materialType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo informado não encontrado."));

        Company company = companyRepository.findById(material.company())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));

        if (material.allDeposits()) {
            List<Deposit> deposits = depositRepository.findAll();
            if (deposits.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum almoxarifado encontrado.");
            }
            for (Deposit deposit : deposits) {
                materialList.add(createMaterial(material, type, company, deposit));
            }
        } else {
            Deposit deposit = depositRepository.findById(material.deposit())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Almoxarifado não encontrado."));
            materialList.add(createMaterial(material, type, company, deposit));
        }

        // Salvar todos os materiais de uma vez
        materialRepository.saveAll(materialList);

        return materialList;
    }

    private Material createMaterial(MaterialRequest material, Type type, Company company, Deposit deposit) {
        Material newMaterial = new Material();
        newMaterial.setMaterialName(material.materialName());
        newMaterial.setMaterialBrand(material.materialBrand());
        newMaterial.setMaterialPower(material.materialPower());
        newMaterial.setMaterialAmps(material.materialAmps());  // Corrigido
        newMaterial.setMaterialLength(material.materialLength());  // Corrigido
        newMaterial.setBuyUnit(material.buyUnit());
        newMaterial.setRequestUnit(material.requestUnit());
//        newMaterial.setStockQuantity(material.stockQt());
        newMaterial.setMaterialType(type);
        newMaterial.setDeposit(deposit);
        newMaterial.setCompany(company);
        return newMaterial;
    }


    private List<MaterialResponse> convertToMaterialResponse(List<Material> materialList) {
        return materialList.stream()
                .map(material -> new MaterialResponse(
                        material.getIdMaterial(),
                        material.getMaterialName(),
                        material.getMaterialBrand(),
                        material.getMaterialPower(),
                        material.getMaterialAmps(),
                        material.getMaterialLength(),
                        material.getBuyUnit(),
                        material.getRequestUnit(),
                        material.getStockQuantity(),
                        material.isInactive(),
                        material.getMaterialType() != null ? material.getMaterialType().getTypeName() : null,
                        material.getMaterialType() != null && material.getMaterialType().getGroup() != null
                                ? material.getMaterialType().getGroup().getGroupName()
                                : null,
                        material.getDeposit() != null ? material.getDeposit().getDepositName() : null,
                        material.getCompany() != null ? material.getCompany().getCompanyName() : null
                ))
                .toList();
    }


    @Transactional
    public ResponseEntity<?> deleteById(Long idMaterial, UUID idUsuario) {
        var user = userRepository.findById(idUsuario);
        var material = materialRepository.findById(idMaterial);
        var log = new Log();

        if (material.isPresent() && user.isPresent()) {
            if (material.get().isInactive()) {
                materialRepository.delete(material.get());
                String logMessage = String.format("Usuário %s excluiu material %d com sucesso.",
                        user.get().getUsername(),
                        material.get().getIdMaterial());

                log.setMessage(logMessage);
                log.setUser(user.get());
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

    @Transactional
    public ResponseEntity<?> update(MaterialRequest material, Long materialId, UUID idUsuario) {
        var user = userRepository.findById(idUsuario);
        var existingMaterial = materialRepository.findById(materialId);

        // Verifica se o material existe
        if (existingMaterial.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Material não encontrado.");
        }

        // Verifica se o usuário existe
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado.");
        }

        // Atualiza os campos necessários do material existente
        Material materialToUpdate = existingMaterial.get();
        materialToUpdate.setMaterialName(material.materialName());
        materialToUpdate.setMaterialBrand(material.materialBrand());

        materialToUpdate.setMaterialPower(material.materialPower());
        materialToUpdate.setMaterialAmps(material.materialAmps());
        materialToUpdate.setMaterialLength(material.materialLength());

        materialToUpdate.setBuyUnit(material.buyUnit());
        materialToUpdate.setRequestUnit(material.requestUnit());
        materialToUpdate.setInactive(material.inactive());
        materialToUpdate.setMaterialType(typeRepository.findById(material.materialType()).orElse(null));

        materialRepository.save(materialToUpdate);

        // Cria e salva o log de atualização
        var log = new Log();
        String logMessage = String.format("Usuário %s atualizou material %d com sucesso.",
                user.get().getUsername(),
                materialId);

        log.setMessage(logMessage);
        log.setUser(user.get());
        log.setCategory("Estoque");
        log.setType("Atualização");
        logRepository.save(log);

        return ResponseEntity.ok(convertToMaterialResponse(List.of(materialToUpdate)));
    }


    public ResponseEntity<List<MaterialDTOMob>> findAllForMobile() {
        var materials = materialRepository.findAllByOrderByMaterialName();
        List<MaterialDTOMob> materialsDTO = new ArrayList<>();

        for (Material m : materials) {
            var companyName = m.getCompany() != null ? m.getCompany().getCompanyName() : "";
            var depositId = m.getDeposit() != null ? m.getDeposit().getIdDeposit() : 0;
            materialsDTO.add(new MaterialDTOMob(
                    m.getIdMaterial(),
                    m.getMaterialName(),
                    m.getMaterialBrand(),
                    m.getMaterialPower(),
                    m.getMaterialAmps(),
                    m.getMaterialLength(),
                    m.getRequestUnit(),
                    String.valueOf(m.getStockAvailable()),
                    companyName,
                    depositId
            ));
        }

        return ResponseEntity.ok(materialsDTO);
    }
}
