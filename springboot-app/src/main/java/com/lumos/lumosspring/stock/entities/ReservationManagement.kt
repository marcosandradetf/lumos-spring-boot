package com.lumos.lumosspring.stock.entities

import com.lumos.lumosspring.execution.entities.DirectExecution
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.user.AppUser
import com.lumos.lumosspring.util.ReservationStatus
import jakarta.persistence.*

@Entity
class ReservationManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val reservationManagementId : Long = 0

    @Column(length = 100)
    var description : String? = null

    @ManyToOne
    @JoinColumn(name = "stockist_id", nullable = false)
    var stockist: AppUser = AppUser()

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "reservationManagement")
    var streets: MutableList<PreMeasurementStreet> = ArrayList()

    var status: String = ReservationStatus.PENDING

}
