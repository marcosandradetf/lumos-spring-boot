package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.TypeDTO;
import com.lumos.lumosspring.stock.entities.Type;
import com.lumos.lumosspring.stock.repository.GroupRepository;
import com.lumos.lumosspring.stock.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeService {
    private final TypeRepository tipoRepository;
    private final GroupRepository groupRepository;
    private final TypeRepository typeRepository;

    public TypeService(TypeRepository tipoRepository, GroupRepository groupRepository, TypeRepository typeRepository) {
        this.tipoRepository = tipoRepository;
        this.groupRepository = groupRepository;
        this.typeRepository = typeRepository;
    }

    public List<Type> findAll() {
        return tipoRepository.findAll();
    }

    public Type findById(Long id) {
        return tipoRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?>    save(TypeDTO typeDTO) {
        Type type = new Type();
        type.setTypeName(typeDTO.typeName());
        type.setGroup(groupRepository.findById(typeDTO.groupId()).orElse(null));
        tipoRepository.save(type);

        return ResponseEntity.ok(typeRepository.findAll());
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
        type.setGroup(groupRepository.findById(typeDTO.groupId()).orElse(null));
        tipoRepository.save(type);

        return ResponseEntity.ok(typeRepository.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var type = typeRepository.findById(id).orElse(null);
        if (type == null) {
            return ResponseEntity.notFound().build();
        }
        typeRepository.delete(type);
        return ResponseEntity.ok(typeRepository.findAll());
    }
}
