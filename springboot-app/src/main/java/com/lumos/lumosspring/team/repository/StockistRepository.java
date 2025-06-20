package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Stockist;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;


public interface StockistRepository extends CrudRepository<Stockist, Long> {
    List<Stockist> findAllByUserId(UUID userId);
}
