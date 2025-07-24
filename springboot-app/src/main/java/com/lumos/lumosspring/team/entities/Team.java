package com.lumos.lumosspring.team.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;


@Table
public class Team {
    @Id
    private Long idTeam;

    private String teamName;

    private UUID driverId;

    private UUID electricianId;

    private String plateVehicle;

    @Column("ufname")
    private String UFName;

    private String cityName;

    private String teamPhone;

    @Column("region_region_id")
    private Long region;

    @Column("deposit_id_deposit")
    private Long depositId;

    public long getIdTeam() {
        return idTeam;
    }

    public void setIdTeam(Long idTeam) {
        this.idTeam = idTeam;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }


    public Team(Long idTeam, String teamName, UUID driverId, UUID electricianId, String plateVehicle, String UFName, String cityName, String teamPhone, Long region, Long depositId) {
        this.idTeam = idTeam;
        this.teamName = teamName;
        this.driverId = driverId;
        this.electricianId = electricianId;
        this.plateVehicle = plateVehicle;
        this.UFName = UFName;
        this.cityName = cityName;
        this.teamPhone = teamPhone;
        this.region = region;
        this.depositId = depositId;
    }

    public Team() {}

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public UUID getElectricianId() {
        return electricianId;
    }

    public void setElectricianId(UUID electricianId) {
        this.electricianId = electricianId;
    }

    public String getPlateVehicle() {
        return plateVehicle;
    }

    public void setPlateVehicle(String plateVehicle) {
        this.plateVehicle = plateVehicle;
    }

    public String getUFName() {
        return UFName;
    }

    public void setUFName(String UFName) {
        this.UFName = UFName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getTeamPhone() {
        return teamPhone;
    }

    public void setTeamPhone(String teamPhone) {
        this.teamPhone = teamPhone;
    }

    public Long getRegion() {
        return region;
    }

    public void setRegion(Long region) {
        this.region = region;
    }

    public Long getDepositId() {
        return depositId;
    }

    public void setDepositId(Long depositId) {
        this.depositId = depositId;
    }


    public String getTeamCode() {
        return getDriverId().toString().concat("_").concat(String.valueOf(idTeam));
    }


}
