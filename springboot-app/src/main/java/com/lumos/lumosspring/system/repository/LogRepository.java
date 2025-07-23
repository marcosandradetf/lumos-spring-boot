package com.lumos.lumosspring.system.repository;

import com.lumos.lumosspring.system.entities.Log;
import org.springframework.data.repository.CrudRepository;

public interface LogRepository extends CrudRepository<Log, Long> {
}
