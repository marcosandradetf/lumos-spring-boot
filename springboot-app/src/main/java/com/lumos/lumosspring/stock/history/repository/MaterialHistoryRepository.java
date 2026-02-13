package com.lumos.lumosspring.stock.history.repository;

import com.lumos.lumosspring.stock.history.model.MaterialHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaterialHistoryRepository extends CrudRepository<MaterialHistory, UUID> {
}
