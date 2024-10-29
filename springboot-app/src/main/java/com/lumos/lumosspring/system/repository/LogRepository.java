package com.lumos.lumosspring.system.repository;

import com.lumos.lumosspring.system.entities.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
}
