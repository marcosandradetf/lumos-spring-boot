package com.lumos.lumosspring.stock.order.teamrequest.controller

import com.lumos.lumosspring.stock.order.teamrequest.dto.OrderRequest
import com.lumos.lumosspring.stock.order.teamrequest.dto.ReplyRequest
import com.lumos.lumosspring.stock.order.teamrequest.service.OrderServiceRegister
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class OrderControllerRegister(
    private val orderServiceRegister: OrderServiceRegister
) {
    @PostMapping("/order/reply")
    fun reply(@RequestBody replyRequest: ReplyRequest): ResponseEntity<Void> {
        return orderServiceRegister.reply(replyRequest)
    }

    @PostMapping("/order/mark-as-collected")
    fun markAsCollected(@RequestBody orders:  List<OrderRequest>): ResponseEntity<Void> {
        return orderServiceRegister.markAsCollected(orders)
    }
}