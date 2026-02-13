package com.lumos.lumosspring.stock.order.controller

import com.lumos.lumosspring.stock.order.service.OrderServiceView
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

    @GetMapping("/order/get-order-history-by-status")
    fun getOrderHistoryByStatus(
        @RequestParam teamId: Long,
        @RequestParam status: String,
        @RequestParam contractReferenceItemId: Long,
    ) = orderServiceView.getOrderHistoryByStatus(teamId, status, contractReferenceItemId)


}