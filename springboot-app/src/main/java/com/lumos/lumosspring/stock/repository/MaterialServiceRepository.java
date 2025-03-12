package com.lumos.lumosspring.stock.repository;

import com.lumos.lumosspring.stock.entities.MaterialService;
import com.lumos.lumosspring.stock.entities.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialServiceRepository extends JpaRepository<MaterialService, Long> { ;
}
