package com.lumos.lumosspring.execution.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_PreMeasurement")
public class PreMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_measurement")
    private long idMeasurement;

    private String city;

}
