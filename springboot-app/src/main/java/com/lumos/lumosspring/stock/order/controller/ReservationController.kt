package com.lumos.lumosspring.stock.order.controller

import com.lumos.lumosspring.stock.order.dto.Replies
import com.lumos.lumosspring.stock.order.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ReservationController(
    private val reservationService: ReservationService
) {


    @PostMapping("/reservation/reply")
    fun reply(@RequestBody replies: Replies): ResponseEntity<Void> {
        return reservationService.reply(replies)
    }


    @PostMapping("/reservation/mark-as-collected")
    fun markAsCollected(@RequestBody reservationIds: List<Long>): ResponseEntity<Void> {
        return reservationService.markAsCollected(reservationIds)
    }

    @GetMapping("/execution/get-reservations-by-status-and-stockist")
    fun getReservationsByStatusAndStockist(
        @RequestParam depositId: Long,
        @RequestParam status: String,
    ) = reservationService.getReservationsByStatusAndStockist(depositId, status)

}