package com.lumos.lumosspring.pre_measurement.entities;

import com.lumos.lumosspring.stock.entities.ReservationManagement;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.user.AppUser;
import com.lumos.lumosspring.util.ExecutionStatus;
import com.lumos.lumosspring.util.ItemStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class PreMeasurementStreet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_measurement_street_id")
    private long preMeasurementStreetId;

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

    @Column(nullable = false)
    private Integer step;

    @OneToMany(mappedBy = "preMeasurementStreet", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreMeasurementStreetItem> items = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // Métodos auxiliares para garantir consistência no relacionamento
    public void addItem(PreMeasurementStreetItem item) {
        items.add(item);
        item.setPreMeasurementStreet(this);
        item.setPreMeasurement(this.preMeasurement);
    }

    @ManyToOne
    @JoinColumn(name = "pre_measurement_id") // FK para a entidade PreMeasurement
    private PreMeasurement preMeasurement;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id")
    private AppUser assignedBy;

    @ManyToOne
    @JoinColumn(name = "finished_by_user_id")
    private AppUser finishedBy;

    private Instant createdAt;
    private Instant assignedAt;
    private Instant finishedAt;

    @ManyToOne
    @JoinColumn(name = "reservation_management_id")
    private ReservationManagement reservationManagement;

    private Boolean prioritized;

    private String comment;
    private String preMeasurementPhotoUri;
    private String executionPhotoUri;

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

    public void cancelAllItems() {
        if (!this.items.isEmpty()) {
            this.items.forEach(item -> {
                item.setItemStatus(ItemStatus.CANCELLED);
            });
        }
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
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

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }

    public AppUser getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(AppUser assignedBy) {
        this.assignedBy = assignedBy;
    }

    public AppUser getFinishedBy() {
        return finishedBy;
    }

    public ReservationManagement getReservationManagement() {
        return reservationManagement;
    }

    public void setReservationManagement(ReservationManagement reservationManagement) {
        this.reservationManagement = reservationManagement;
    }

    public void setFinishedBy(AppUser finishedBy) {
        this.finishedBy = finishedBy;
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

    public void assignToStockistAndTeam(Team team, AppUser assignedBy, Instant assignedAt, boolean prioritized, String comment, ReservationManagement reservationManagement) {
        this.team = team;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.prioritized = prioritized;
        this.comment = comment;
        this.reservationManagement = reservationManagement;
        this.streetStatus = ExecutionStatus.WAITING_STOCKIST;
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
}
