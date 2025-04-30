package com.lumos.lumosspring.stock.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_supplier")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(columnDefinition = "TEXT")
    private String supplierName;
    @Column(columnDefinition = "TEXT")
    private String supplierCnpj;
    @Column(columnDefinition = "TEXT")
    private String supplierContact;
    @Column(columnDefinition = "TEXT")
    private String supplierAddress;
    @Column(columnDefinition = "TEXT")
    private String supplierPhone;
    @Column(columnDefinition = "TEXT")
    private String supplierEmail;

    public Supplier() { }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierContact() {
        return supplierContact;
    }

    public void setSupplierContact(String supplierContact) {
        this.supplierContact = supplierContact;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public void setSupplierAddress(String supplierAddress) {
        this.supplierAddress = supplierAddress;
    }

    public String getSupplierPhone() {
        return supplierPhone;
    }

    public void setSupplierPhone(String supplierPhone) {
        this.supplierPhone = supplierPhone;
    }

    public String getSupplierEmail() {
        return supplierEmail;
    }

    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail = supplierEmail;
    }

    public String getSupplierCnpj() {
        return supplierCnpj;
    }

    public void setSupplierCnpj(String supplierCnpj) {
        this.supplierCnpj = supplierCnpj;
    }
}
