package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.team.Team;
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

    public long getIdStreet() {
        return idStreet;
    }

    public void setIdStreet(long idStreet) {
        this.idStreet = idStreet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
