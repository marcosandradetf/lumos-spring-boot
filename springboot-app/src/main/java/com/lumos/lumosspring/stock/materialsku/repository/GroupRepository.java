package com.lumos.lumosspring.stock.materialsku.repository;

import com.lumos.lumosspring.stock.materialsku.model.MaterialGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends CrudRepository<MaterialGroup, Long> {
    List<MaterialGroup> findAllByOrderByIdGroupAsc();

    boolean existsByGroupName(String groupName);
}
