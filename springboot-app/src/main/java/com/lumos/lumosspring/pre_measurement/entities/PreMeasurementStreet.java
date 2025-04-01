package com.lumos.lumosspring.pre_measurement.entities;

import com.lumos.lumosspring.util.ItemStatus;
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

    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;

    private double latitude;

    private double longitude;

    private String streetStatus = ItemStatus.PENDING;

    @OneToMany(mappedBy = "preMeasurementStreet", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreMeasurementStreetItem> items = new HashSet<>();

    // Métodos auxiliares para garantir consistência no relacionamento
    public void addItem(PreMeasurementStreetItem item) {
        items.add(item);
        item.setPreMeasurementStreet(this);
    }

    public void removeItem(PreMeasurementStreetItem item) {
        items.remove(item);
        item.setPreMeasurementStreet(null);
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getStreetStatus() {
        return streetStatus;
    }

    public void setStreetStatus(String status) {
        this.streetStatus = status;
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

}
