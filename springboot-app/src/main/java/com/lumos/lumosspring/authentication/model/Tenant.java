package com.lumos.lumosspring.authentication.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table
public class Tenant {
    @Id
    UUID tenantId;
    Long tenant_number;
    String tenantName;
    String bucket;

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenant_number() {
        return tenant_number;
    }

    public void setTenant_number(Long tenant_number) {
        this.tenant_number = tenant_number;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
