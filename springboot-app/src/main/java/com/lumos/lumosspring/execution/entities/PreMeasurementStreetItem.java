package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.stock.entities.Material;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pre_measurements_streets_items")
public class PreMeasurementStreetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_measurement_street_item_id")
    private long preMeasurementStreetItemId;

    @ManyToOne
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne
    @JoinColumn(name = "pre_measurement_street_id")
    private PreMeasurementStreet preMeasurementStreet;

    private double itemQuantity;

    private Status itemStatus;

    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @ManyToOne
    private PreMeasurementStreetItemService service;

    public long getPreMeasurementStreetItemId() {
        return preMeasurementStreetItemId;
    }

    public void setPreMeasurementStreetItemId(long itemId) {
        this.preMeasurementStreetItemId = itemId;
    }

    public double getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(double itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal itemValue) {
        this.unitPrice = itemValue;
        if (itemValue != null) {
            setTotalPrice(itemValue.multiply(BigDecimal.valueOf(itemQuantity)));
            preMeasurementStreet.getPreMeasurement().sumTotalPrice(totalPrice);
        }
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal itemTotalValue) {
        this.totalPrice = itemTotalValue;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public PreMeasurementStreet getPreMeasurement() {
        return preMeasurementStreet;
    }

    public void setPreMeasurement(PreMeasurementStreet measurement) {
        this.preMeasurementStreet = measurement;
    }

    public Status getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(Status itemStatus) {
        this.itemStatus = itemStatus;
    }

    public void addItemQuantity(double v) {
        this.itemQuantity += v;
    }

    public PreMeasurementStreet getPreMeasurementStreet() {
        return preMeasurementStreet;
    }

    public void setPreMeasurementStreet(PreMeasurementStreet preMeasurementStreet) {
        this.preMeasurementStreet = preMeasurementStreet;
    }

    public PreMeasurementStreetItemService getService() {
        return service;
    }

    public void setService(PreMeasurementStreetItemService service) {
        this.service = service;
    }

    public enum Status {
        PENDING,
        CANCELLED,
        APPROVED,
    }


}
