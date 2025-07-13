package com.lumos.lumosspring.stock.service

import com.lumos.lumosspring.stock.entities.OrderMaterial
import com.lumos.lumosspring.stock.entities.OrderMaterialItem
import com.lumos.lumosspring.stock.repository.OrderMaterialItemRepository
import com.lumos.lumosspring.stock.repository.OrderMaterialRepository
import com.lumos.lumosspring.stock.repository.StockQueryRepository
import com.lumos.lumosspring.stock.repository.TeamQueryRepository
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class StockService(
    private val stockQueryRepository: StockQueryRepository,
    private val orderMaterialRepository: OrderMaterialRepository,
    private val orderMaterialItemRepository: OrderMaterialItemRepository,
    private val teamQueryRepository: TeamQueryRepository,
) {

    fun getMaterialsForMaintenance(strUUID: String): ResponseEntity<StockQueryRepository.StockResponse> {
        val uuid = try {
            UUID.fromString(strUUID)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(ex.message)
        }

        val depositId = stockQueryRepository.getTruckDepositId(uuid)
            ?: throw IllegalArgumentException("Deposito do caminhão não encontrado no sistema.")

        return ResponseEntity.ok(stockQueryRepository.getMaterialsForMaintenance(depositId))
    }

    fun saveOrder(
        strUUID: String,
        order: StockQueryRepository.OrderWithItems
    ): ResponseEntity<StockQueryRepository.StockResponse> {
        var userUuid: UUID
        var orderUuid: UUID
        var createdAt: Instant

        try {
            userUuid = UUID.fromString(strUUID)
            orderUuid = UUID.fromString(order.orderMaterial.orderId)
            createdAt = Instant.parse(order.orderMaterial.createdAt)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(ex.message)
        }

        val teamId = teamQueryRepository.getTeamIdByUserId(userUuid)
            ?: throw IllegalArgumentException("StockService - saveOrder - Team ID do not exist.")

        val newOrder = OrderMaterial(
            orderId = orderUuid,
            orderCode = order.orderMaterial.orderCode,
            createdAt = createdAt,
            depositId = order.orderMaterial.depositId,
            status = ReservationStatus.PENDING,
            teamId = teamId,
            isNewEntry = true
        )

        val exists = orderMaterialRepository.existsById(orderUuid)

        if(exists) {
            return ResponseEntity.noContent().build()
        }

        orderMaterialRepository.save(newOrder)

        val items = order.items.map {
            OrderMaterialItem(
                orderId = orderUuid,
                materialId = it.materialId,
                isNewEntry = true
            )
        }

        orderMaterialItemRepository.saveAll(items)

        return ResponseEntity.noContent().build()
    }

}