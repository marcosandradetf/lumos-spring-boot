package com.lumos.lumosspring.stock.materialsku.service;

import com.lumos.lumosspring.stock.materialsku.model.MaterialGroup;
import com.lumos.lumosspring.stock.materialsku.repository.GroupRepository;
import com.lumos.lumosspring.stock.materialsku.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GroupService {
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private TypeRepository typeRepository;

    @Cacheable("getAllGroups")
    public List<MaterialGroup> findAll() {
        return groupRepository.findAllByOrderByIdGroupAsc();
    }

    public MaterialGroup findById(Long id) {
        return groupRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(String groupName) {
        if (groupRepository.existsByGroupName(groupName)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Este grupo já existe."));
        }

        var group = new MaterialGroup(
                null,
                groupName
        );
        groupRepository.save(group);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> update(Long typeId , String groupName) {
        var group = groupRepository.findById(typeId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        group = new MaterialGroup(
                group.getIdGroup(),
                groupName
        );
        groupRepository.save(group);


        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var group = groupRepository.findById(id).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        if (typeRepository.existsGroup(id).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Não é possível excluir: há tipos associados a este grupo."));
        }

        groupRepository.delete(group);
        return ResponseEntity.ok(this.findAll());
    }
}
