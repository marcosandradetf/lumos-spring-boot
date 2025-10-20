package com.lumos.lumosspring.stock.order.controller

import com.lumos.lumosspring.stock.order.dto.Replies
import com.lumos.lumosspring.stock.order.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class OrderController(
    private val orderService: OrderService
) {


    @PostMapping("/order/reply")
    fun reply(@RequestBody replies: Replies): ResponseEntity<Void> {
        return orderService.reply(replies)
    }


    @PostMapping("/order/mark-as-collected")
    fun markAsCollected(@RequestBody reservationIds: List<Long>): ResponseEntity<Void> {
        return orderService.markAsCollected(reservationIds)
    }

    @GetMapping("/order/get-orders-by-status-and-stockist")
    fun getReservationsByStatusAndStockist(
        @RequestParam depositId: Long,
        @RequestParam status: String,
    ) = orderService.getReservationsByStatusAndStockist(depositId, status)

}