package com.lumos.lumosspring.stock.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table
public class Deposit {
    @Id
    private Long idDeposit;

    private String depositName;

    private String depositAddress;

    private String depositDistrict;

    private String depositCity;

    private String depositState;

    private String depositPhone;

    private Boolean isTruck;

    @Column("company_id")
    private Long companyId;

    @Column("region_id")
    private Long region;

    public Deposit(Long idDeposit, String depositName, String depositAddress, String depositDistrict, String depositCity, String depositState, String depositPhone, Long companyId, Long region) {
        this.idDeposit = idDeposit;
        this.depositName = depositName;
        this.depositAddress = depositAddress;
        this.depositDistrict = depositDistrict;
        this.depositCity = depositCity;
        this.depositState = depositState;
        this.depositPhone = depositPhone;
        this.companyId = companyId;
        this.region = region;
    }

    public Deposit() {}

    public Long getIdDeposit() {
        return idDeposit;
    }

    public void setIdDeposit(Long idDeposit) {
        this.idDeposit = idDeposit;
    }

    public String getDepositName() {
        return depositName;
    }

    public void setDepositName(String depositName) {
        this.depositName = depositName;
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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getRegion() {
        return region;
    }

    public void setRegion(Long region) {
        this.region = region;
    }

    public Boolean getTruck() {
        return isTruck;
    }

    public void setTruck(Boolean truck) {
        isTruck = truck;
    }
}
