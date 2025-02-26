package com.lumos.lumosspring.execution.entities;

import com.lumos.lumosspring.stock.entities.MaterialStock;
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
    @JoinColumn(name = "material_stock_id")
    private MaterialStock materialStock;

    @ManyToOne
    private Street street;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int quantityCompleted;

    private ReservationStatus status; // Pode ser "pendente", "coletado", "cancelado"

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

    public MaterialStock getMaterialStock() {
        return materialStock;
    }

    public void setMaterialStock(MaterialStock material) {
        this.materialStock = material;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        this.materialStock.removeStockAvailable(reservedQuantity);
    }

    public int getQuantityCompleted() {
        return quantityCompleted;
    }

    public void setQuantityCompleted(int quantityCompleted) {
        this.quantityCompleted = quantityCompleted;
        this.materialStock.removeStockQuantity(quantityCompleted);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public enum ReservationStatus {
        PENDING,
        COLLECTED,
        CANCELLED
    }

}


