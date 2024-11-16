package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
