package com.lumos.lumosspring.installation.model.premeasurement;

import com.lumos.lumosspring.authentication.model.TenantEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table
public class PreMeasurement extends TenantEntity {
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

    private Instant signDate;
    private String responsible;
    private String signatureUri;

    private String comment;

    private Instant availableAt;
    private Instant reportViewAt;
    private Instant finishedAt;

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

    public Instant getSignDate() {
        return signDate;
    }

    public void setSignDate(Instant signDate) {
        this.signDate = signDate;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getSignatureUri() {
        return signatureUri;
    }

    public void setSignatureUri(String signatureUri) {
        this.signatureUri = signatureUri;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Instant getReportViewAt() {
        return reportViewAt;
    }

    public void setReportViewAt(Instant reportViewAt) {
        this.reportViewAt = reportViewAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}


