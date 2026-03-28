package com.lumos.lumosspring.plan.model;

import java.io.Serializable;
import java.util.Objects;

public class PlanModuleId implements Serializable {

    private String planName;
    private String moduleCode;

    public PlanModuleId() {
    }

    public PlanModuleId(String planName, String moduleCode) {
        this.planName = planName;
        this.moduleCode = moduleCode;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlanModuleId that = (PlanModuleId) o;
        return Objects.equals(planName, that.planName) && Objects.equals(moduleCode, that.moduleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planName, moduleCode);
    }
}
