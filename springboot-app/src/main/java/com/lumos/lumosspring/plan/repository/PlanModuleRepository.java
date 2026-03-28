package com.lumos.lumosspring.plan.repository;

import com.lumos.lumosspring.plan.model.PlanModule;
import com.lumos.lumosspring.plan.model.PlanModuleId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlanModuleRepository {

    private static final RowMapper<PlanModule> ROW_MAPPER = (rs, rowNum) -> {
        PlanModule row = new PlanModule();
        row.setPlanName(rs.getString("plan_name"));
        row.setModuleCode(rs.getString("module_code"));
        row.setEnabled(rs.getObject("enabled", Boolean.class));
        return row;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public PlanModuleRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PlanModule> findById(PlanModuleId id) {
        String sql = "SELECT plan_name, module_code, enabled FROM plan_module WHERE plan_name = :planName AND module_code = :moduleCode";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("planName", id.getPlanName())
                .addValue("moduleCode", id.getModuleCode());
        List<PlanModule> list = jdbc.query(sql, params, ROW_MAPPER);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public boolean existsById(PlanModuleId id) {
        return findById(id).isPresent();
    }

    public List<PlanModule> findByPlanName(String planName) {
        String sql = "SELECT plan_name, module_code, enabled FROM plan_module WHERE plan_name = :planName ORDER BY module_code";
        return jdbc.query(sql, new MapSqlParameterSource("planName", planName), ROW_MAPPER);
    }

    public List<PlanModule> findByModuleCode(String moduleCode) {
        String sql = "SELECT plan_name, module_code, enabled FROM plan_module WHERE module_code = :moduleCode ORDER BY plan_name";
        return jdbc.query(sql, new MapSqlParameterSource("moduleCode", moduleCode), ROW_MAPPER);
    }

    public void insert(PlanModule row) {
        String sql = """
                INSERT INTO plan_module (plan_name, module_code, enabled)
                VALUES (:planName, :moduleCode, :enabled)
                """;
        jdbc.update(sql, toParams(row));
    }

    public void update(PlanModule row) {
        String sql = """
                UPDATE plan_module SET enabled = :enabled
                WHERE plan_name = :planName AND module_code = :moduleCode
                """;
        jdbc.update(sql, toParams(row));
    }

    public void deleteById(PlanModuleId id) {
        String sql = "DELETE FROM plan_module WHERE plan_name = :planName AND module_code = :moduleCode";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("planName", id.getPlanName())
                .addValue("moduleCode", id.getModuleCode());
        jdbc.update(sql, params);
    }

    private static MapSqlParameterSource toParams(PlanModule row) {
        Boolean enabled = row.getEnabled() != null ? row.getEnabled() : true;
        return new MapSqlParameterSource()
                .addValue("planName", row.getPlanName())
                .addValue("moduleCode", row.getModuleCode())
                .addValue("enabled", enabled);
    }
}
