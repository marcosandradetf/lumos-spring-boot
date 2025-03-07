package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.stock.entities.Material;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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

    @OneToMany(mappedBy = "preMeasurementStreetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreMeasurementStreetItemService> services = new HashSet<>();

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

    public void addService(PreMeasurementStreetItemService service) {
        this.services.add(service);
    }

    public void removeService(String serviceName) {
        this.services.stream()
                .filter(s -> s.getService().getServiceName().equals(serviceName))
                .findFirst()
                .ifPresent(s -> {
                    this.services.remove(s);
                });
    }

    public PreMeasurementStreetItemService getService(String serviceName) {
        return this.services.stream()
                .filter(s -> s.getService().getServiceName().equals(serviceName))
                .findFirst()
                .orElse(null);
    }



    public enum Status {
        PENDING,
        CANCELLED,
        APPROVED,
    }

}
