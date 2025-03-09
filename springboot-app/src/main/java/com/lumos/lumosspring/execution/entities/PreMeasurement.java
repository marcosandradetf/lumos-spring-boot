package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.team.Region;
import com.lumos.lumosspring.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tb_pre_measurements")
public class PreMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_measurement_id")
    private long preMeasurementId;

    @OneToMany(mappedBy = "preMeasurement", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreMeasurementStreet> streets = new HashSet<>();

    private String city;

    @Column(columnDefinition = "TEXT")
    private String htmlReport;

    // Métodos auxiliares para garantir consistência no relacionamento
    public void addStreet(PreMeasurementStreet street) {
        streets.add(street);
        street.setPreMeasurement(this);
    }

    public void removeStreet(PreMeasurementStreet street) {
        streets.remove(street);
        street.setPreMeasurement(null);
    }

    @ManyToOne
    private Region region;

    private Status status;

    private Type typePreMeasurement;

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

    private BigDecimal totalPrice = BigDecimal.ZERO;

    public long getPreMeasurementId() {
        return preMeasurementId;
    }

    public void setPreMeasurementId(long preMeasurementId) {
        this.preMeasurementId = preMeasurementId;
    }

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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Type getTypePreMeasurement() {
        return typePreMeasurement;
    }

    public void setTypePreMeasurement(Type typePreMeasurement) {
        this.typePreMeasurement = typePreMeasurement;
    }

    public Set<PreMeasurementStreet> getStreets() {
        return streets;
    }

    public void setStreets(Set<PreMeasurementStreet> streets) {
        this.streets = streets;
    }

    public void sumTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = this.totalPrice.add(totalPrice);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getHtmlReport() {
        return htmlReport;
    }

    public void setHtmlReport(String htmlReport) {
        this.htmlReport = htmlReport;
    }

    public enum Status {
        PENDING,
        VALIDATING,
        WAITING,
        VALIDATED,
        CANCELLED,
        IN_PROGRESS,
        FINISHED,
    }

    public enum Type {
        INSTALLATION,
        MAINTENANCE,
    }
}
