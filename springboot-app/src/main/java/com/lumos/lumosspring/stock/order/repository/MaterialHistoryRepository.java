package com.lumos.lumosspring.stock.order.repository;

import com.lumos.lumosspring.stock.order.model.MaterialHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaterialHistoryRepository extends CrudRepository<MaterialHistory, UUID> {
}
