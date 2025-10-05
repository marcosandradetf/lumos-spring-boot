package com.lumos.lumosspring.stock.materialreference.service;

import com.lumos.lumosspring.stock.materialreference.dto.TypeDTO;
import com.lumos.lumosspring.stock.materialreference.model.MaterialType;
import com.lumos.lumosspring.stock.materialreference.repository.GroupRepository;
import com.lumos.lumosspring.stock.materialreference.repository.MaterialReferenceRepository;
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRepository;
import com.lumos.lumosspring.stock.materialreference.repository.TypeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TypeService {
    private final TypeRepository tipoRepository;
    private final GroupRepository groupRepository;
    private final TypeRepository typeRepository;
    private final MaterialStockRepository materialStockRepository;

    public TypeService(TypeRepository tipoRepository, GroupRepository groupRepository, TypeRepository typeRepository, MaterialReferenceRepository materialReferenceRepository, MaterialStockRepository materialStockRepository) {
        this.tipoRepository = tipoRepository;
        this.groupRepository = groupRepository;
        this.typeRepository = typeRepository;
        this.materialStockRepository = materialStockRepository;
    }

    @Cacheable("getAllTypes")
    public List<MaterialType> findAll() {
        return tipoRepository.findAllByOrderByIdTypeAsc();
    }

    public MaterialType findById(Long id) {
        return tipoRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(TypeDTO typeDTO) {
        if (typeRepository.existsByTypeName(typeDTO.typeName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Este tipo já existe."));
        }

        MaterialType materialType = new MaterialType();
        materialType.setTypeName(typeDTO.typeName());
        materialType.setIdGroup(typeDTO.groupId());
        tipoRepository.save(materialType);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> update(Long typeId ,TypeDTO typeDTO) {
        var type = typeRepository.findById(typeId).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }

        type.setTypeName(typeDTO.typeName());
        type.setIdGroup(typeDTO.groupId());
        tipoRepository.save(type);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var type = typeRepository.findById(id).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }

        if (materialStockRepository.existsType(id).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Não é possível excluir: há materiais associados a este tipo."));
        }

        typeRepository.delete(type);
        return ResponseEntity.ok(this.findAll());
    }
}
