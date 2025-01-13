package com.lumos.lumosspring.stock.entities;

import com.lumos.lumosspring.team.Region;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "tb_deposits")
public class Deposit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idDeposit;
    @Column(columnDefinition = "TEXT", unique = true, nullable = false)
    private String depositName;

    private String depositAddress;

    private String depositDistrict;

    private String depositCity;

    private String depositState;

    private String depositPhone;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    public long getIdDeposit() {
        return idDeposit;
    }

    public void setIdDeposit(long idDeposit) {
        this.idDeposit = idDeposit;
    }

    public String getDepositName() {
        return depositName;
    }

    public void setDepositName(String depositName) {
        this.depositName = depositName;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
    public Company getCompany() {
        return company;
    }

    public String getDepositAddress() {
        return depositAddress;
    }

    public void setDepositAddress(String depositAddress) {
        this.depositAddress = depositAddress;
    }

    public String getDepositDistrict() {
        return depositDistrict;
    }

    public void setDepositDistrict(String depositDistrict) {
        this.depositDistrict = depositDistrict;
    }

    public String getDepositCity() {
        return depositCity;
    }

    public void setDepositCity(String depositCity) {
        this.depositCity = depositCity;
    }

    public String getDepositState() {
        return depositState;
    }

    public void setDepositState(String depositState) {
        this.depositState = depositState;
    }

    public String getDepositPhone() {
        return depositPhone;
    }

    public void setDepositPhone(String depositPhone) {
        this.depositPhone = depositPhone;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}
