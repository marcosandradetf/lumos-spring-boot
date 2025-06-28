package com.lumos.lumosspring.reservation.controller

import com.lumos.lumosspring.reservation.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ReservationController(
    private val reservationService: ReservationService
) {

    data class ReserveItem(val reserveId: Long)
    data class Replies(
        val approved: List<ReserveItem>,
        val rejected: List<ReserveItem>
    )
    @PostMapping("/reservation/reply")
    fun reply(@RequestBody replies: Replies): ResponseEntity<Void> {
        return reservationService.reply(replies)
    }


}