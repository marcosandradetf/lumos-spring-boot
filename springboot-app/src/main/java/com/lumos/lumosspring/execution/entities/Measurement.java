package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.team.Region;
import com.lumos.lumosspring.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "tb_measurements")
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measurement_id")
    private long measurementId;

    private String description;

    private String address;

    private String city;

    private double latitude;

    private double longitude;

    @ManyToOne
    private Region region;

    @ManyToOne
    private Deposit deposit;

    private Status status;

    private String deviceId;

    private Type typeMeasurement;

    @ManyToOne
    @JoinColumn(name = "created_by_id_user")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id_user")
    private User assignedBy;

    @ManyToOne
    @JoinColumn(name = "finished_by_id_user")
    private User finishedBy;

    private Instant createdAt;
    private Instant assignedAt;
    private Instant finishedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public User getFinishedBy() {
        return finishedBy;
    }

    public void setFinishedBy(User finishedBy) {
        this.finishedBy = finishedBy;
    }

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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Deposit getDeposit() {
        return deposit;
    }

    public void setDeposit(Deposit deposit) {
        this.deposit = deposit;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Type getTypeMeasurement() {
        return typeMeasurement;
    }

    public void setTypeMeasurement(Type typeMeasurement) {
        this.typeMeasurement = typeMeasurement;
    }

    public enum Status {
        PENDING,
        IN_PROGRESS,
        CANCELLED,
        FINISHED,
    }

    public enum Type {
        INSTALLATION,
        MAINTENANCE,
    }
}
