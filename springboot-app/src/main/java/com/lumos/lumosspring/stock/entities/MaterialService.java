package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_materials_services")
public class MaterialService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private long serviceId;
    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String serviceName;

    public long getServiceId() {
        return serviceId;
    }

    public void setServiceId(long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }


}

