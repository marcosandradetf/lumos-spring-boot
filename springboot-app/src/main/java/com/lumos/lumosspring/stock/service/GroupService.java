package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.controller.dto.TypeDTO;
import com.lumos.lumosspring.stock.entities.Group;
import com.lumos.lumosspring.stock.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {
    @Autowired
    private GroupRepository groupRepository;

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Group findById(Long id) {
        return groupRepository.findById(id).orElse(null);
    }

    public ResponseEntity<?> save(String groupName) {
        var group = new Group();
        group.setGroupName(groupName);
        groupRepository.save(group);

        return ResponseEntity.ok(groupRepository.findAll());
    }

    public ResponseEntity<?> update(Long typeId , String groupName) {
        var group = groupRepository.findById(typeId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        group.setGroupName(groupName);
        groupRepository.save(group);

        return ResponseEntity.ok(groupRepository.findAll());
    }

    public ResponseEntity<?> delete(Long id) {
        var group = groupRepository.findById(id).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }
        groupRepository.delete(group);
        return ResponseEntity.ok(groupRepository.findAll());
    }
}
