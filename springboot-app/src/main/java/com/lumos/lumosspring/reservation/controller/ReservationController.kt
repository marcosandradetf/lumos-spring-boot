package com.lumos.lumosspring.reservation.controller

import com.lumos.lumosspring.reservation.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class ReservationController(
    private val reservationService: ReservationService
) {

    data class ReserveItem(val reserveId: Long)

    data class OrderItem(val orderItemId: UUID)

    data class Replies(
        val approvedReserves: List<ReserveItem>,
        val rejectedReserves: List<ReserveItem>,

        val approvedOrders: List<OrderItem>,
        val rejectedOrders: List<OrderItem>,
    )

    data class RepliesReserves(
        val approved: List<ReserveItem>,
        val rejected: List<ReserveItem>,
    )
    data class RepliesOrders(
        val approved: List<OrderItem>,
        val rejected: List<OrderItem>,
    )
    @PostMapping("/reservation/reply")
    fun reply(@RequestBody replies: Replies): ResponseEntity<Void> {
        return reservationService.reply(replies)
    }


    @PostMapping("/reservation/mark-as-collected")
    fun markAsCollected(@RequestBody reservationIds: List<Long>): ResponseEntity<Void> {
        return reservationService.markAsCollected(reservationIds)
    }


}