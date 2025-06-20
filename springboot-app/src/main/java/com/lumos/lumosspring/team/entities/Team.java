package com.lumos.lumosspring.team.entities;

import com.lumos.lumosspring.stock.entities.Deposit;
import com.lumos.lumosspring.user.AppUser;
import jakarta.persistence.*;

import java.util.List;


@Entity
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_team")
    private long idTeam;

    @Column(columnDefinition = "TEXT")
    private String teamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private AppUser driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "electrician_id", nullable = false)
    private AppUser electrician;

    @ManyToMany
    @JoinTable(
            name = "team_complementary_member",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<AppUser> complementaryMembers;

    private String plateVehicle;

    @Column(columnDefinition = "TEXT")
    private String UFName;

    @Column(columnDefinition = "TEXT")
    private String cityName;

    private String teamPhone;

    @ManyToOne
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    private Deposit deposit;

    public long getIdTeam() {
        return idTeam;
    }

    public void setIdTeam(long idTeam) {
        this.idTeam = idTeam;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public AppUser getDriver() {
        return driver;
    }

    public void setDriver(AppUser driver) {
        this.driver = driver;
    }

    public AppUser getElectrician() {
        return electrician;
    }

    public void setElectrician(AppUser electrician) {
        this.electrician = electrician;
    }

    public List<AppUser> getComplementaryMembers() {
        return complementaryMembers;
    }

    public void setComplementaryMembers(List<AppUser> complementaryMembers) {
        this.complementaryMembers = complementaryMembers;
    }
    public String getPlateVehicle() {
        return plateVehicle;
    }

    public void setPlateVehicle(String plateVehicle) {
        this.plateVehicle = plateVehicle;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
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

    public Deposit getDeposit() {
        return deposit;
    }

    public void setDeposit(Deposit deposit) {
        this.deposit = deposit;
    }

    public String getTeamCode() {
        return driver.getUserId().toString().concat("_").concat(String.valueOf(idTeam));
    }

    public String getTeamPhone() {
        return teamPhone;
    }

    public void setTeamPhone(String teamPhone) {
        this.teamPhone = teamPhone;
    }

}
