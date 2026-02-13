package com.lumos.lumosspring.stock.order.service

import com.lumos.lumosspring.stock.order.dto.OrderResponse
import com.lumos.lumosspring.stock.order.dto.OrdersByCaseResponse
import com.lumos.lumosspring.stock.order.repository.OrderMaterialRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.collections.iterator

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
                    description = description ?: "Desconhecido",
                    teamName = orders.first().teamName,
                    orders = list
                )
            )
        }

        return ResponseEntity.ok().body(response)
    }

    fun getOrderHistoryByStatus(teamId: Long, status: String, contractReferenceItemId: Long) : ResponseEntity<Any> {
        val response = orderMaterialRepository.getOrderHistoryByStatus(teamId, status, contractReferenceItemId)
        return ResponseEntity.ok().body(response)
    }
}