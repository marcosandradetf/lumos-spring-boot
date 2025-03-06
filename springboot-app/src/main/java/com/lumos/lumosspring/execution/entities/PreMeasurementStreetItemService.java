package com.lumos.lumosspring.execution.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pre_measurements_streets_items_services")
public class PreMeasurementStreetItemService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long preMeasurementServiceId;

    private String preMeasurementServiceName;

    private String preMeasurementServiceDescription;

    private float serviceQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    public Long getPreMeasurementServiceId() {
        return preMeasurementServiceId;
    }

    public void setPreMeasurementServiceId(Long preMeasurementServiceId) {
        this.preMeasurementServiceId = preMeasurementServiceId;
    }

    public String getPreMeasurementServiceName() {
        return preMeasurementServiceName;
    }

    public void setPreMeasurementServiceName(String preMeasurementServiceName) {
        this.preMeasurementServiceName = preMeasurementServiceName;
    }

    public String getPreMeasurementServiceDescription() {
        return preMeasurementServiceDescription;
    }

    public void setPreMeasurementServiceDescription(String preMeasurementServiceDescription) {
        this.preMeasurementServiceDescription = preMeasurementServiceDescription;
    }

    public float getServiceQuantity() {
        return serviceQuantity;
    }

    public void setServiceQuantity(float serviceQuantity) {
        this.serviceQuantity = serviceQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setUnitPrice(BigDecimal itemValue, PreMeasurementStreetItem streetItem) {
        this.unitPrice = itemValue;
        if (itemValue != null) {
            setTotalPrice(itemValue.multiply(BigDecimal.valueOf(serviceQuantity)));
            streetItem.getPreMeasurementStreet().getPreMeasurement().sumTotalPrice(totalPrice);
        }
    }
}
