package com.lumos.lumosspring.team;

import com.lumos.lumosspring.stock.entities.Deposit;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "tb_regions")
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long regionId;

    private String regionName;

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(long regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
}
