package com.lumos.lumosspring.pre_measurement.entities;

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

    @ManyToOne
    @JoinColumn(name = "pre_measurement_id")
    private PreMeasurement preMeasurementStreet;

    @ManyToOne
    private Region region;

    private Status status;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    @ManyToOne
    @JoinColumn(name = "finished_by_user_id")
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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }


    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
