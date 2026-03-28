package com.lumos.lumosspring.plan.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("plan")
public class Plan {

    @Id
    private String planName;

    private String description;
    private BigDecimal pricePerUserMonthly;
    private BigDecimal pricePerUserYearly;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    public Plan() {
    }

    public Plan(String planName, String description, BigDecimal pricePerUserMonthly, BigDecimal pricePerUserYearly, Boolean isActive, OffsetDateTime createdAt) {
        this.planName = planName;
        this.description = description;
        this.pricePerUserMonthly = pricePerUserMonthly;
        this.pricePerUserYearly = pricePerUserYearly;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPricePerUserMonthly() {
        return pricePerUserMonthly;
    }

    public void setPricePerUserMonthly(BigDecimal pricePerUserMonthly) {
        this.pricePerUserMonthly = pricePerUserMonthly;
    }

    public BigDecimal getPricePerUserYearly() {
        return pricePerUserYearly;
    }

    public void setPricePerUserYearly(BigDecimal pricePerUserYearly) {
        this.pricePerUserYearly = pricePerUserYearly;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
