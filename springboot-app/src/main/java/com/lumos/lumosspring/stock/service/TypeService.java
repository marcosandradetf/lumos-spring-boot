package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.TypeDTO;
import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.stock.repository.GroupRepository;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import com.lumos.lumosspring.stock.repository.ProductStockRepository;
import com.lumos.lumosspring.stock.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ProductStockRepository materialStockRepository;

    public TypeService(TypeRepository tipoRepository, GroupRepository groupRepository, TypeRepository typeRepository, MaterialRepository materialRepository, ProductStockRepository materialStockRepository) {
        this.tipoRepository = tipoRepository;
        this.groupRepository = groupRepository;
        this.typeRepository = typeRepository;
        this.materialStockRepository = materialStockRepository;
    }

    public List<Type> findAll() {
        return tipoRepository.findAllByOrderByIdTypeAsc();
    }

    public Type findById(Long id) {
        return tipoRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(TypeDTO typeDTO) {
        if (typeRepository.existsByTypeName(typeDTO.typeName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Este tipo já existe."));
        }

        Type type = new Type();
        type.setTypeName(typeDTO.typeName());
        type.setGroup(groupRepository.findById(typeDTO.groupId()).orElse(null));
        tipoRepository.save(type);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> update(Long typeId ,TypeDTO typeDTO) {
        var type = typeRepository.findById(typeId).orElse(null);
        var group = groupRepository.findById(typeDTO.groupId()).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        type.setTypeName(typeDTO.typeName());
        type.setGroup(group);
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
