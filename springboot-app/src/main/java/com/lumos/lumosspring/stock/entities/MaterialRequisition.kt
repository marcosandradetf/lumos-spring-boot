package com.lumos.lumosspring.stock.entities

import com.lumos.lumosspring.contract.entities.ContractReferenceItem
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurement
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "tb_material_requisitions")
class MaterialRequisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var materialRequisitionId: Long = 0

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "deposit_id")
    var deposit: Deposit = Deposit()

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "contract_reference_item_id")
    var contractReferenceItem : ContractReferenceItem = ContractReferenceItem()

    @ManyToOne(cascade = [(CascadeType.MERGE)])
    @JoinColumn(name = "pre_measurement_street_id")
    var street: PreMeasurementStreet = PreMeasurementStreet()

    @Column(nullable = false)
    var quantityRequested: Double = 0.0

    var statusRequisition: String = "PENDING"
}