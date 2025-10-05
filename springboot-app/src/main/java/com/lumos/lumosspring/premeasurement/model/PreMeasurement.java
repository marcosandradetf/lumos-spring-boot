package com.lumos.lumosspring.premeasurement.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table
public class PreMeasurement {
    @Id
    private Long preMeasurementId;

    private UUID devicePreMeasurementId;

    private String city;

    private String htmlReport;

    @Column("contract_contract_id")
    private Long contractId;

    @Column("region_region_id")
    private Long regionId;

    private String status;

    private String typePreMeasurement;

    private BigDecimal totalPrice = BigDecimal.ZERO;

    private Integer step = 0;

    private UUID createdByUserId;

    private Instant createdAt;

    private Long reservationManagementId;

    public Long getPreMeasurementId() {
        return preMeasurementId;
    }

    public void setPreMeasurementId(Long preMeasurementId) {
        this.preMeasurementId = preMeasurementId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHtmlReport() {
        return htmlReport;
    }

    public void setHtmlReport(String htmlReport) {
        this.htmlReport = htmlReport;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTypePreMeasurement() {
        return typePreMeasurement;
    }

    public void setTypePreMeasurement(String typePreMeasurement) {
        this.typePreMeasurement = typePreMeasurement;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer steps) {
        this.step = steps;
    }

    public UUID getDevicePreMeasurementId() {
        return devicePreMeasurementId;
    }

    public void setDevicePreMeasurementId(UUID devicePreMeasurementId) {
        this.devicePreMeasurementId = devicePreMeasurementId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getReservationManagementId() {
        return reservationManagementId;
    }

    public void setReservationManagementId(Long reservationManagementId) {
        this.reservationManagementId = reservationManagementId;
    }
}


