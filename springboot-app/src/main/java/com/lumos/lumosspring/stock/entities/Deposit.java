package com.lumos.lumosspring.stock.entities;

import com.lumos.lumosspring.team.entities.Region;
import com.lumos.lumosspring.team.entities.Stockist;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.user.User;
import jakarta.persistence.*;

import java.util.List;
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

    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MaterialStock> materialStocks;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToMany(mappedBy = "deposit")
    private List<Stockist> stockists;

    @OneToMany(mappedBy = "deposit")
    private List<Team> teams;

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

    public Set<MaterialStock> getMaterialStocks() {
        return materialStocks;
    }

    public void setMaterialStocks(Set<MaterialStock> productStocks) {
        this.materialStocks = productStocks;
    }

    public List<Stockist> getStockists() {
        return stockists;
    }

    public void setStockists(List<Stockist> stockists) {
        this.stockists = stockists;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }
}
