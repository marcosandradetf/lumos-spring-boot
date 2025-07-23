package com.lumos.lumosspring.pre_measurement.entities;


import com.lumos.lumosspring.util.ExecutionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table
public class PreMeasurementStreet {
    @Id
    private Long preMeasurementStreetId;

    private Long deviceStreetId;

    private String deviceId;

    private String description;

    private String lastPower;

    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;

    private Double latitude;
    private Double longitude;

    private String streetStatus = ExecutionStatus.PENDING;

    private Integer step;

    private Long teamId;

    private Long preMeasurementId;

    @Column("created_by_user_id")
    private UUID createdById;

    @Column("assigned_by_user_id")
    private UUID assignedById;

    @Column("finished_by_user_id")
    private UUID finishedById;

    private Instant createdAt;
    private Instant assignedAt;
    private Instant finishedAt;

    private Long reservationManagementId;

    private Boolean prioritized;

    private String comment;
    private String preMeasurementPhotoUri;
    private String executionPhotoUri;


    public long getPreMeasurementStreetId() {
        return preMeasurementStreetId;
    }

    public void setPreMeasurementStreetId(long preMeasurementStreetId) {
        this.preMeasurementStreetId = preMeasurementStreetId;
    }

    public Long getDeviceStreetId() {
        return deviceStreetId;
    }

    public void setDeviceStreetId(Long deviceStreetId) {
        this.deviceStreetId = deviceStreetId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLastPower() {
        return lastPower;
    }

    public void setLastPower(String lastPower) {
        this.lastPower = lastPower;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStreetStatus() {
        return streetStatus;
    }

    public void setStreetStatus(String streetStatus) {
        this.streetStatus = streetStatus;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getPreMeasurementId() {
        return preMeasurementId;
    }

    public void setPreMeasurementId(Long preMeasurementId) {
        this.preMeasurementId = preMeasurementId;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public UUID getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(UUID assignedById) {
        this.assignedById = assignedById;
    }

    public UUID getFinishedById() {
        return finishedById;
    }

    public void setFinishedById(UUID finishedById) {
        this.finishedById = finishedById;
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

    public Long getReservationManagementId() {
        return reservationManagementId;
    }

    public void setReservationManagementId(Long reservationManagementId) {
        this.reservationManagementId = reservationManagementId;
    }

    public Boolean getPrioritized() {
        return prioritized;
    }

    public void setPrioritized(Boolean prioritized) {
        this.prioritized = prioritized;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPreMeasurementPhotoUri() {
        return preMeasurementPhotoUri;
    }

    public void setPreMeasurementPhotoUri(String preMeasurementPhotoUri) {
        this.preMeasurementPhotoUri = preMeasurementPhotoUri;
    }

    public String getExecutionPhotoUri() {
        return executionPhotoUri;
    }

    public void setExecutionPhotoUri(String executionPhotoUri) {
        this.executionPhotoUri = executionPhotoUri;
    }
}
