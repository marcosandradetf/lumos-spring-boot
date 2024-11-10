package com.lumos.lumosspring.estoque.service;

import com.lumos.lumosspring.authentication.entities.Role;
import com.lumos.lumosspring.authentication.repository.UserRepository;
import com.lumos.lumosspring.estoque.controller.dto.MaterialRequest;
import com.lumos.lumosspring.estoque.controller.dto.MaterialResponse;
import com.lumos.lumosspring.estoque.model.Material;
import com.lumos.lumosspring.estoque.model.Tipo;
import com.lumos.lumosspring.estoque.repository.AlmoxarifadoRepository;
import com.lumos.lumosspring.estoque.repository.EmpresaRepository;
import com.lumos.lumosspring.estoque.repository.TipoRepository;
import com.lumos.lumosspring.system.entities.Log;
import com.lumos.lumosspring.system.repository.LogRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.lumos.lumosspring.estoque.repository.MaterialRepository;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final TipoRepository tipoRepository;
    private final AlmoxarifadoRepository almoxarifadoRepository;
    private final EmpresaRepository empresaRepository;

    public MaterialService(MaterialRepository materialRepository, UserRepository userRepository, LogRepository logRepository, TipoRepository tipoRepository, AlmoxarifadoRepository almoxarifadoRepository, EmpresaRepository empresaRepository) {
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.tipoRepository = tipoRepository;
        this.almoxarifadoRepository = almoxarifadoRepository;
        this.empresaRepository = empresaRepository;
    }

    public List<Material> findAll() {
        return materialRepository.findAll();
    }

    public Material findById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    @Transactional
    public ResponseEntity<?> save(MaterialRequest material) {
        // Validação do material
        ResponseEntity<String> validationError = validateMaterialRequest(material);
        if (validationError != null) {
            return validationError;
        }
        // Conversão de MaterialRequest para Material
        Material newMaterial = convertToMaterial(material);
        materialRepository.save(newMaterial);

        //return ResponseEntity.ok("Material cadastrado com sucesso.");
        return ResponseEntity.ok(convertToMaterialResponse(newMaterial));
    }

    private ResponseEntity<String> validateMaterialRequest(MaterialRequest material) {
        if (materialRepository.existsByNomeMaterial(material.nomeMaterial())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Já existe um material com o nome fornecido.");
        }
        if (!tipoRepository.existsById(material.tipoMaterial())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Tipo de material não encontrado.");
        }
        if (!almoxarifadoRepository.existsById(material.almoxarifado())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Almoxarifado não encontrado.");
        }
        if (!empresaRepository.existsById(material.empresa())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Empresa não encontrada.");
        }
        return null; // Indica que não houve erros
    }

    private Material convertToMaterial(MaterialRequest material) {
        var newMaterial = new Material();
        newMaterial.setNomeMaterial(material.nomeMaterial());
        newMaterial.setMarcaMaterial(material.marcaMaterial());
        newMaterial.setUnidadeCompra(material.unidadeCompra());
        newMaterial.setUnidadeRequisicao(material.unidadeRequisicao());
        newMaterial.setQtdeEstoque(material.qtdeEstoque());
        newMaterial.setInativo(material.inativo());
        newMaterial.setTipoMaterial(tipoRepository.findById(material.tipoMaterial()).orElse(null));
        newMaterial.setAlmoxarifado(almoxarifadoRepository.findById(material.almoxarifado()).orElse(null));
        newMaterial.setEmpresa(empresaRepository.findById(material.empresa()).orElse(null));
        return newMaterial;
    }

    private MaterialResponse convertToMaterialResponse(Material material) {
        return new MaterialResponse(
                material.getIdMaterial(),
                material.getNomeMaterial(),
                material.getMarcaMaterial(),
                material.getUnidadeCompra(),
                material.getUnidadeRequisicao(),
                material.getQtdeEstoque(),
                material.isInativo(),
                material.getTipoMaterial().getNomeTipo(),
                material.getTipoMaterial().getGrupo().getNomeGrupo(),
                material.getAlmoxarifado().getNomeAlmoxarifado(),
                material.getEmpresa().getNomeEmpresa()
        );
    }

    @Transactional
    public ResponseEntity<String> deleteById(Long idMaterial, UUID idUsuario) {
        var user = userRepository.findById(idUsuario);
        var material = materialRepository.findById(idMaterial);
        var log = new Log();

        if (material.isPresent() && user.isPresent()) {
            if (material.get().isInativo()) {
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
    public ResponseEntity<String> update(Material material, UUID idUsuario) {
        var user = userRepository.findById(idUsuario);
        var existingMaterial = materialRepository.findById(material.getIdMaterial());

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
        materialToUpdate.setNomeMaterial(material.getNomeMaterial());
        materialToUpdate.setMarcaMaterial(material.getMarcaMaterial());
        materialToUpdate.setUnidadeCompra(material.getUnidadeCompra());
        materialToUpdate.setUnidadeRequisicao(material.getUnidadeRequisicao());
        materialToUpdate.setInativo(material.isInativo());
        materialToUpdate.setQtdeEstoque(material.getQtdeEstoque());
        materialToUpdate.setTipoMaterial(material.getTipoMaterial());
        materialToUpdate.setEmpresa(material.getEmpresa());
        materialToUpdate.setAlmoxarifado(material.getAlmoxarifado());

        materialRepository.save(materialToUpdate);

        // Cria e salva o log de atualização
        var log = new Log();
        String logMessage = String.format("Usuário %s atualizou material %d com sucesso.",
                user.get().getUsername(),
                material.getIdMaterial());

        log.setMessage(logMessage);
        log.setUser(user.get());
        log.setCategory("Estoque");
        log.setType("Atualização");
        logRepository.save(log);

        return ResponseEntity.ok("Material atualizado com sucesso.");
    }

}
