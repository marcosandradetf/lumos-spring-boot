package com.lumos.lumosspring.team.entities

import com.lumos.lumosspring.stock.entities.Deposit
import com.lumos.lumosspring.user.User
import jakarta.persistence.*

@Entity
class Stockist {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val stockistId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    val deposit: Deposit = Deposit()

    @OneToOne(fetch = FetchType.LAZY)
    val user: User = User()

}