package com.lumos.lumosspring.stock.materialstock.model;

import org.springframework.data.annotation.Id;

public class Supplier {
    @Id
    private Long supplierId;

    private String supplierName;

    private String supplierCnpj;

    private String supplierContact;

    private String supplierAddress;

    private String supplierPhone;

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
