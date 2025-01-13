package com.lumos.lumosspring.team;

import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.user.User;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;


@Entity
@Table(name = "tb_team")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_team")
    private long idTeam;

    @Column(columnDefinition = "TEXT")
    private String teamName;

    @OneToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_team_measurement",
            joinColumns = @JoinColumn(name = "id_team"),
            inverseJoinColumns = @JoinColumn(name = "id_measurement")
    )
    private List<PreMeasurement> preMeasurement;

    @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_users_regions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id")
    )
    private Set<Region> regions;

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

    public List<PreMeasurement> getPreMeasurement() {
        return preMeasurement;
    }

    public void setPreMeasurement(List<PreMeasurement> preMeasurement) {
        this.preMeasurement = preMeasurement;
    }

    public Set<Region> getRegions() {
        return regions;
    }

    public void setRegions(Set<Region> regions) {
        this.regions = regions;
    }
}
