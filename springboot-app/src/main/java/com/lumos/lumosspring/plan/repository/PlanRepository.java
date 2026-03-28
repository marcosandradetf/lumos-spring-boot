package com.lumos.lumosspring.plan.repository;

import com.lumos.lumosspring.plan.model.Plan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends CrudRepository<Plan, String> {

    List<Plan> findAllByOrderByPlanNameAsc();

    List<Plan> findAllByIsActiveTrueOrderByPlanNameAsc();
}
