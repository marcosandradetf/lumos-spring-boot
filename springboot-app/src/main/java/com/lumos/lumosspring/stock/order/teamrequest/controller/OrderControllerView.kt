package com.lumos.lumosspring.stock.order.teamrequest.controller

import com.lumos.lumosspring.stock.order.teamrequest.service.OrderServiceView
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class OrderControllerView(
    private val orderServiceView: OrderServiceView
) {
    @GetMapping("/order/get-orders-by-status-and-stockist")
    fun getReservationsByStatusAndStockist(
        @RequestParam depositId: Long,
        @RequestParam status: String,
    ) = orderServiceView.getReservationsByStatusAndStockist(depositId, status)
}