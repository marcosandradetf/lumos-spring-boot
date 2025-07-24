package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Stockist;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface StockistRepository extends CrudRepository<Stockist, Long> {
    List<Stockist> findAllByUserId(UUID userId);
}
