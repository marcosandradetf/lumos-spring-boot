package com.lumos.lumosspring.stock.entities

import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.user.User
import jakarta.persistence.*

@Entity
@Table(name = "tb_reservation_managements")
class ReservationManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private val id : Long = 0

    var description : String = ""

    @ManyToOne
    @JoinColumn(name = "stockist_id", nullable = false)
    var stockist: User = User()

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "reservation")
    var streets: MutableList<PreMeasurementStreet> = ArrayList()


}
