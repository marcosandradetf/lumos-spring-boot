package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.stock.entities.Material;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_material_reservation")
public class MaterialReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material_reservation")
    private long idMaterialReservation;

    @Column(columnDefinition = "TEXT")
    private String stockReservationName;

    @ManyToOne
    private Material material;

    @ManyToOne
    private PreMeasurement preMeasurement;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int quantityCompleted;

    private String status; // Pode ser "pendente", "coletado", "cancelado"

    public long getIdMaterialReservation() {
        return idMaterialReservation;
    }

    public void setIdMaterialReservation(long idMaterialReservation) {
        this.idMaterialReservation = idMaterialReservation;
    }

    public String getStockReservationName() {
        return stockReservationName;
    }

    public void setStockReservationName(String stockReservationName) {
        this.stockReservationName = stockReservationName;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public PreMeasurement getPreMeasurement() {
        return preMeasurement;
    }

    public void setPreMeasurement(PreMeasurement preMeasurement) {
        this.preMeasurement = preMeasurement;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        this.material.removeStockAvailable(reservedQuantity);
    }

    public int getQuantityCompleted() {
        return quantityCompleted;
    }

    public void setQuantityCompleted(int quantityCompleted) {
        this.quantityCompleted = quantityCompleted;
        this.material.removeStockQuantity(quantityCompleted);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
