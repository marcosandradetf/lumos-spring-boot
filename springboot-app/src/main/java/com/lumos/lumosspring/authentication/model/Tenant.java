package com.lumos.lumosspring.authentication.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table
public class Tenant {
    @Id
    UUID tenantId;
    Long tenantNumber;
    String description;
    String bucket;

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantNumber() {
        return tenantNumber;
    }

    public void setTenantNumber(Long tenantNumber) {
        this.tenantNumber = tenantNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
