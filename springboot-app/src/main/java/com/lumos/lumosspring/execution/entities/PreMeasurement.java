package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.team.Region;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "tb_PreMeasurement")
public class PreMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measurement_id")
    private long measurementId;

    private String description;

    private String UF;
    private String city;

    @ManyToOne
    private Region region;

    @OneToMany
    List<Street> streets;

    public long getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(long measurementId) {
        this.measurementId = measurementId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUF() {
        return UF;
    }

    public void setUF(String UF) {
        this.UF = UF;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public List<Street> getStreets() {
        return streets;
    }

    public void setStreets(List<Street> streets) {
        this.streets = streets;
    }
}
