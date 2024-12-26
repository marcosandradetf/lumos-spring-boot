package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.TypeDTO;
import com.lumos.lumosspring.stock.entities.Group;
import com.lumos.lumosspring.stock.repository.GroupRepository;
import com.lumos.lumosspring.stock.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<Group> findAll() {
        return groupRepository.findAllByOrderByIdGroupAsc();
    }

    public Group findById(Long id) {
        return groupRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(String groupName) {
        if (groupRepository.existsByGroupName(groupName)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "Este grupo já existe."));
        }

        var group = new Group();
        group.setGroupName(groupName);
        groupRepository.save(group);

        return ResponseEntity.ok(this.findAll());
    }

    public ResponseEntity<?> update(Long typeId , String groupName) {
        var group = groupRepository.findById(typeId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        group.setGroupName(groupName);
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
