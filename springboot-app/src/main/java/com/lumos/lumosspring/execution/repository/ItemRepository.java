package com.lumos.lumosspring.execution.repository;

import com.lumos.lumosspring.execution.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
