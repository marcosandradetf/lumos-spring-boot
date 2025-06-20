package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.MaterialGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<MaterialGroup, Long> {
    List<MaterialGroup> findAllByOrderByIdGroupAsc();

    boolean existsByGroupName(String groupName);
}
