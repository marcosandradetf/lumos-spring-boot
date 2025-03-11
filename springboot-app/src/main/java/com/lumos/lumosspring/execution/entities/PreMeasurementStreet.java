package com.lumos.lumosspring.execution.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_pre_measurements_streets")
public class PreMeasurementStreet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_measurement_street_id")
    private long preMeasurementStreetId;

    private String description;

    private String lastPower;

    private String address;
    private String street;

    private double latitude;

    private double longitude;

    private Status status;

    @OneToMany(mappedBy = "preMeasurementStreet", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreMeasurementStreetItem> items = new HashSet<>();

    // Métodos auxiliares para garantir consistência no relacionamento
    public void addItem(PreMeasurementStreetItem item) {
        items.add(item);
        item.setPreMeasurement(this);
    }

    public void removeItem(PreMeasurementStreetItem item) {
        items.remove(item);
        item.setPreMeasurement(null);
    }

    @ManyToOne
    @JoinColumn(name = "pre_measurement_id") // FK para a entidade PreMeasurement
    private PreMeasurement preMeasurement;

    public long getPreMeasurementStreetId() {
        return preMeasurementStreetId;
    }

    public void setPreMeasurementStreetId(long preMeasurementId) {
        this.preMeasurementStreetId = preMeasurementId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public PreMeasurement getPreMeasurement() {
        return preMeasurement;
    }

    public void setPreMeasurement(PreMeasurement preMeasurement) {
        this.preMeasurement = preMeasurement;
    }

    public String getLastPower() {
        return lastPower;
    }

    public void setLastPower(String lastPower) {
        this.lastPower = lastPower;
    }

    public Set<PreMeasurementStreetItem> getItems() {
        return items;
    }

    public void setItems(Set<PreMeasurementStreetItem> items) {
        this.items = items;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public enum Status {
        PENDING,
        IN_PROGRESS,
        CANCELLED,
        FINISHED,
    }

}
