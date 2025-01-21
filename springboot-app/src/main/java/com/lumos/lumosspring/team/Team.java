package com.lumos.lumosspring.team;

import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.entities.Street;
import com.lumos.lumosspring.user.User;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;


@Entity
@Table(name = "tb_teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_team")
    private long idTeam;

    @Column(columnDefinition = "TEXT")
    private String teamName;

    @ManyToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

//    @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
//    @JoinTable(
//            name = "tb_team_measurement",
//            joinColumns = @JoinColumn(name = "id_team"),
//            inverseJoinColumns = @JoinColumn(name = "id_measurement")
//    )
//    private List<PreMeasurement> preMeasurement;

    @Column(columnDefinition = "TEXT")
    private String UFName;

    @Column(columnDefinition = "TEXT")
    private String cityName;

    @ManyToOne
    private Region region;

    @ManyToOne
    private Street street;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

//    public List<PreMeasurement> getPreMeasurement() {
//        return preMeasurement;
//    }
//
//    public void setPreMeasurement(List<PreMeasurement> preMeasurement) {
//        this.preMeasurement = preMeasurement;
//    }

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

    public Street getStreet() {
        return street;
    }

    public void setStreet(Street street) {
        this.street = street;
    }
}
