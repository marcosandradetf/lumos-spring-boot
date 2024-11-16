package com.lumos.lumosspring.execution.entities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "tb_street")
public class Street {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_street")
    private long idStreet;

    @Column(columnDefinition = "TEXT")
    private String name;

    @OneToMany
    @JoinColumn(name = "id_item")
    private List<Item> items;


}
