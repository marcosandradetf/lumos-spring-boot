package com.lumos.lumosspring.maintenance.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_maintenance")
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long maintenance_id;

    private String description;

    private String public_place;
    private String neighborhood;
    private String city;

}
