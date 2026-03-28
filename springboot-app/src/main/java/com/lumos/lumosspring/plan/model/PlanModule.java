package com.lumos.lumosspring.plan.model;

/**
 * Associação plano ↔ módulo ({@code plan_module}).
 * Chave composta persistida manualmente (Spring Data JDBC 3.x não mapeia composite PK via aggregate template).
 */
public class PlanModule {

    private String planName;
    private String moduleCode;
    private Boolean enabled;

    public PlanModule() {
    }

    public PlanModule(String planName, String moduleCode, Boolean enabled) {
        this.planName = planName;
        this.moduleCode = moduleCode;
        this.enabled = enabled;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public PlanModuleId toId() {
        return new PlanModuleId(planName, moduleCode);
    }
}
