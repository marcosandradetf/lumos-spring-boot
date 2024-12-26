package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findAllByOrderByIdGroupAsc();

    boolean existsByGroupName(String groupName);
}
