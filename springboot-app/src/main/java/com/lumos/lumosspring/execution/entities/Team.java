package com.lumos.lumosspring.execution.entities;

import jakarta.persistence.*;

import java.util.List;


@Entity
@Table(name = "tb_team")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_team")
    private long idTeam;

    @Column(columnDefinition = "TEXT")
    private String teamName;

    @Column(columnDefinition = "TEXT")
    private String region;

    @ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_team_measurement",
            joinColumns = @JoinColumn(name = "id_team"),
            inverseJoinColumns = @JoinColumn(name = "id_measurement")
    )
    private List<PreMeasurement> preMeasurement;

}
