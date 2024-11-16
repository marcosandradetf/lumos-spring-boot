package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_deposits")
public class Deposit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idDeposit;
    @Column(columnDefinition = "TEXT", unique = true)
    private String depositName;

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
}
