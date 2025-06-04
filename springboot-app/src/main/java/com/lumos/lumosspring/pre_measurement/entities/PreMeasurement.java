package com.lumos.lumosspring.pre_measurement.entities;

import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.team.entities.Region;
import com.lumos.lumosspring.user.User;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_pre_measurements")
public class PreMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_measurement_id")
    private Long preMeasurementId;

    @OneToMany(mappedBy = "preMeasurement", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreMeasurementStreet> streets = new HashSet<>();

    private String city;

    @Column(columnDefinition = "TEXT")
    private String htmlReport;

    @ManyToOne(fetch = FetchType.LAZY)
    private Contract contract;

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

    private String status;

    private String typePreMeasurement;

    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer steps = 0;

    public Long getPreMeasurementId() {
        return preMeasurementId;
    }

    public void setPreMeasurementId(long preMeasurementId) {
        this.preMeasurementId = preMeasurementId;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTypePreMeasurement() {
        return typePreMeasurement;
    }

    public void setTypePreMeasurement(String typePreMeasurement) {
        this.typePreMeasurement = typePreMeasurement;
    }

    public Set<PreMeasurementStreet> getStreets() {
        return streets;
    }

    public void setStreets(Set<PreMeasurementStreet> streets) {
        this.streets = streets;
    }

    public void sumTotalPrice(BigDecimal value) {
        this.totalPrice = this.totalPrice.add(value);
    }

    public void subtractTotalPrice(BigDecimal value) {
        this.totalPrice = this.totalPrice.subtract(value);
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

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Integer getSteps() {
        return steps;
    }

    public void newStep() {
        if (steps == null) {
            steps = 0;
        }
        steps++;
    }

}


