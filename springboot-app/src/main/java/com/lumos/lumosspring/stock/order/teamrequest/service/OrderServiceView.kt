package com.lumos.lumosspring.stock.order.teamrequest.service

import com.lumos.lumosspring.stock.order.teamrequest.dto.OrderResponse
import com.lumos.lumosspring.stock.order.teamrequest.dto.OrdersByCaseResponse
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class OrderServiceView(
    private val orderMaterialRepository: OrderMaterialRepository
) {

    fun getReservationsByStatusAndStockist(depositId: Long, status: String): ResponseEntity<Any> {
        val response: MutableList<OrdersByCaseResponse> = mutableListOf()
        val ordersByStatus = orderMaterialRepository.getOrdersByStatus(depositId, status)
        val ordersGrouped = ordersByStatus
            .groupBy {
                it.contractor ?: it.description
            }

        for ((description, orders) in ordersGrouped) {
            val list = mutableListOf<OrderResponse>()
            for (order in orders) {
                list.add(
                    OrderResponse(
                        reserveId = order.materialIdReservation,
                        orderId = order.orderId,

                        materialId = order.materialId,

                        requestQuantity = order.requestQuantity,
                        stockQuantity = order.stockQuantity,
                        materialName = order.materialName,
                        description = order.description,
                        status = order.status,
                    )
                )
            }

            response.add(
                OrdersByCaseResponse(
                    description = description,
                    teamName = orders.first().teamName,
                    orders = list
                )
            )
        }

        return ResponseEntity.ok().body(response)
    }
}