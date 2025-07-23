package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.MaterialGroup;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface GroupRepository extends CrudRepository<MaterialGroup, Long> {
    List<MaterialGroup> findAllByOrderByIdGroupAsc();

    boolean existsByGroupName(String groupName);
}
