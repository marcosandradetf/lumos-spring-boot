package com.lumos.lumosspring.stock.service;

import com.lumos.lumosspring.stock.entities.Group;
import com.lumos.lumosspring.stock.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Group save(Group material) {
        return groupRepository.save(material);
    }

    public void deleteById(Long id) {
        groupRepository.deleteById(id);
    }
}
