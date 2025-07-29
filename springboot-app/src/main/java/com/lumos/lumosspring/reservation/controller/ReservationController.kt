package com.lumos.lumosspring.reservation.controller

import com.lumos.lumosspring.dto.reservation.Replies
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


    @PostMapping("/reservation/reply")
    fun reply(@RequestBody replies: Replies): ResponseEntity<Void> {
        return reservationService.reply(replies)
    }


    @PostMapping("/reservation/mark-as-collected")
    fun markAsCollected(@RequestBody reservationIds: List<Long>): ResponseEntity<Void> {
        return reservationService.markAsCollected(reservationIds)
    }


}