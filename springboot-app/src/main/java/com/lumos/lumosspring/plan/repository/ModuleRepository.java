package com.lumos.lumosspring.plan.repository;

import com.lumos.lumosspring.plan.model.Module;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends CrudRepository<Module, String> {

    List<Module> findAllByOrderByModuleCodeAsc();
}
