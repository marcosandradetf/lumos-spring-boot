package com.lumos.lumosspring.stock.entities

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.user.User
import com.lumos.lumosspring.util.ReservationStatus
import jakarta.persistence.*

@Entity
@Table(name = "tb_reservation_managements")
class ReservationManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val reservationManagementId : Long = 0

    @Column(length = 100)
    var description : String = ""

    @ManyToOne
    @JoinColumn(name = "stockist_id", nullable = false)
    var stockist: User = User()

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "reservationManagement")
    var streets: MutableList<PreMeasurementStreet> = ArrayList()

    var status: String = ReservationStatus.PENDING

}
