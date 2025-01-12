package com.lumos.lumosspring.user;

import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "tb_regions")
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long RegionId;

    private String RegionName;

    @ManyToMany(mappedBy = "regions")
    private Set<User> Users;

    public long getRegionId() {
        return RegionId;
    }

    public void setRegionId(long regionId) {
        RegionId = regionId;
    }

    public String getRegionName() {
        return RegionName;
    }

    public void setRegionName(String regionName) {
        RegionName = regionName;
    }

    public Set<User> getUsers() {
        return Users;
    }

    public void setUsers(Set<User> users) {
        Users = users;
    }
}
