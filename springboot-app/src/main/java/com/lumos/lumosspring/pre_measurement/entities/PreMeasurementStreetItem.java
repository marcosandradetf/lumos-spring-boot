package com.lumos.lumosspring.pre_measurement.entities;

import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative;
import com.lumos.lumosspring.util.ItemStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pre_measurements_streets_items")
public class PreMeasurementStreetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_measurement_street_item_id")
    private long preMeasurementStreetItemId;

//    @ManyToOne
//    @JoinColumn(name = "material_id")
//    private Material material;

    @ManyToOne
    @JoinColumn(name = "contract_item_id")
    private ContractItemsQuantitative contractItem;

    @ManyToOne
    @JoinColumn(name = "pre_measurement_street_id")
    private PreMeasurementStreet preMeasurementStreet;

    @ManyToOne
    @JoinColumn(name = "pre_measurement_id")
    private PreMeasurement preMeasurement;

    private double measuredItemQuantity;

    private String itemStatus = ItemStatus.PENDING;

    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;

    private String contractServiceIdMask;
    private BigDecimal contractServiceDividerPrices = BigDecimal.ZERO;

    public long getPreMeasurementStreetItemId() {
        return preMeasurementStreetItemId;
    }

    public void setPreMeasurementStreetItemId(long itemId) {
        this.preMeasurementStreetItemId = itemId;
    }

    public Double getMeasuredItemQuantity() {
        return measuredItemQuantity;
    }

    public void setMeasuredItemQuantity(double itemQuantity) {
        this.measuredItemQuantity = itemQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal itemValue) {
        this.unitPrice = itemValue;
        if (itemValue != null) {
            setTotalPrice(itemValue.multiply(BigDecimal.valueOf(measuredItemQuantity)));
        }
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal itemTotalValue) {
        this.totalPrice = itemTotalValue;
    }

//    public Material getMaterial() {
//        return material;
//    }
//
//    public void setMaterial(Material material) {
//        this.material = material;
//    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public void addItemQuantity(double v) {
        this.measuredItemQuantity += v;
    }

    public void addItemQuantity(double v, boolean updateValue) {
        this.measuredItemQuantity += v;
        if (updateValue) {
            setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(measuredItemQuantity)));
        }
    }

    public PreMeasurementStreet getPreMeasurementStreet() {
        return preMeasurementStreet;
    }

    public void setPreMeasurementStreet(PreMeasurementStreet preMeasurementStreet) {
        this.preMeasurementStreet = preMeasurementStreet;
    }

    public ContractItemsQuantitative getContractItem() {
        return contractItem;
    }

    public void setContractItem(ContractItemsQuantitative contractItem) {
        this.contractItem = contractItem;
        this.setUnitPrice(contractItem.getUnitPrice());
    }

    public String getContractServiceIdMask() {
        return contractServiceIdMask;
    }

    public void setContractServiceIdMask(Long contractServiceId) {
        if (this.contractServiceIdMask == null || this.contractServiceIdMask.isEmpty()) {
            this.contractServiceIdMask = contractServiceId.toString();
        } else {
            this.contractServiceIdMask = this.contractServiceIdMask.concat("#").concat(contractServiceId.toString());
        }
    }

    public void clearContractServices() {
        this.contractServiceIdMask = null;
        this.contractServiceDividerPrices = BigDecimal.ZERO;
    }

    public PreMeasurement getPreMeasurement() {
        return preMeasurement;
    }

    public void setPreMeasurement(PreMeasurement preMeasurement) {
        this.preMeasurement = preMeasurement;
    }

    public void setContractServiceIdMask(String contractServiceIdMask) {
        this.contractServiceIdMask = contractServiceIdMask;
    }

    public BigDecimal getContractServiceDividerPrices() {
        return contractServiceDividerPrices;
    }

    public void setContractServiceDividerPrices(BigDecimal contractServiceDividerPrices) {
        this.contractServiceDividerPrices = this.contractServiceDividerPrices.add(contractServiceDividerPrices);
    }

    public void setContractServiceDividerPrices(BigDecimal contractServiceDividerPrices, Boolean set) {
        this.contractServiceDividerPrices = contractServiceDividerPrices;
    }
}
