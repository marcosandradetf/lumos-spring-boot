package com.lumos.lumosspring.premeasurement.model;


import com.lumos.lumosspring.util.ItemStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table
public class PreMeasurementStreetItem {
    @Id
    private Long preMeasurementStreetItemId;

    private Long contractItemId;

    private Long preMeasurementStreetId;

    private Long preMeasurementId;

    private BigDecimal measuredItemQuantity;

    private String itemStatus = ItemStatus.PENDING;

    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;

    private String contractServiceIdMask;
    private BigDecimal contractServiceDividerPrices = BigDecimal.ZERO;


    public Long getPreMeasurementStreetItemId() {
        return preMeasurementStreetItemId;
    }

    public void setPreMeasurementStreetItemId(Long preMeasurementStreetItemId) {
        this.preMeasurementStreetItemId = preMeasurementStreetItemId;
    }

    public Long getContractItemId() {
        return contractItemId;
    }

    public void setContractItemId(Long contractItemId) {
        this.contractItemId = contractItemId;
    }

    public Long getPreMeasurementStreetId() {
        return preMeasurementStreetId;
    }

    public void setPreMeasurementStreetId(Long preMeasurementStreetId) {
        this.preMeasurementStreetId = preMeasurementStreetId;
    }

    public Long getPreMeasurementId() {
        return preMeasurementId;
    }

    public void setPreMeasurementId(Long preMeasurementId) {
        this.preMeasurementId = preMeasurementId;
    }

    public BigDecimal getMeasuredItemQuantity() {
        return measuredItemQuantity;
    }

    public void setMeasuredItemQuantity(BigDecimal measuredItemQuantity) {
        this.measuredItemQuantity = measuredItemQuantity;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getContractServiceIdMask() {
        return contractServiceIdMask;
    }

    public void setContractServiceIdMask(String contractServiceIdMask) {
        this.contractServiceIdMask = contractServiceIdMask;
    }

    public BigDecimal getContractServiceDividerPrices() {
        return contractServiceDividerPrices;
    }

    public void setContractServiceDividerPrices(BigDecimal contractServiceDividerPrices) {
        this.contractServiceDividerPrices = contractServiceDividerPrices;
    }
}
