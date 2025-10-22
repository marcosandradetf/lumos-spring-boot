package com.lumos.lumosspring.stock.order.installationrequest.repository;

import com.lumos.lumosspring.stock.order.installationrequest.model.MaterialHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaterialHistoryRepository extends CrudRepository<MaterialHistory, UUID> {
}
