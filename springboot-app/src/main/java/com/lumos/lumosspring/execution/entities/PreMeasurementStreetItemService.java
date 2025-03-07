package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.stock.entities.MaterialService;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pre_measurements_streets_items_services")
public class PreMeasurementStreetItemService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long preMeasurementServiceId;

    private String preMeasurementServiceDescription;

    private float serviceQuantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private MaterialService materialService;

    @ManyToOne
    @JoinColumn(name = "pre_measurement_street_item_id")
    private PreMeasurementStreetItem preMeasurementStreetItem;

    public Long getPreMeasurementServiceId() {
        return preMeasurementServiceId;
    }

    public void setPreMeasurementServiceId(Long preMeasurementServiceId) {
        this.preMeasurementServiceId = preMeasurementServiceId;
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

    public void addServiceQuantity(float serviceQuantity) {
        this.serviceQuantity += serviceQuantity;
    }

    public void removeServiceQuantity(float serviceQuantity) {
        this.serviceQuantity -= serviceQuantity;
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

    public void setService(MaterialService materialService) {
        this.materialService = materialService;
    }

    public MaterialService getService() {
        return materialService;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public PreMeasurementStreetItem getPreMeasurementStreetItem() {
        return preMeasurementStreetItem;
    }

    public void setPreMeasurementStreetItem(PreMeasurementStreetItem preMeasurementStreetItem) {
        this.preMeasurementStreetItem = preMeasurementStreetItem;
    }
}
