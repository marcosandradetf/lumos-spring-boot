package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
