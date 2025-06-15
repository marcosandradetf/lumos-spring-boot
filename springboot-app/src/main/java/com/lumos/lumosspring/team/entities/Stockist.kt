package com.lumos.lumosspring.team.entities

import com.lumos.lumosspring.stock.entities.Deposit
import com.lumos.lumosspring.user.User
import jakarta.persistence.*
import java.util.stream.DoubleStream

@Entity
@Table(name = "tb_stockists")
class Stockist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val stockistId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    val deposit: Deposit = Deposit()

    @ManyToOne(fetch = FetchType.LAZY)
    val user: User = User()

    fun getStockistCode(): String {
        return "${stockistId}_${user.idUser}"
    }
}