package com.lumos.lumosspring.stock.materialstock.service


import com.lumos.lumosspring.stock.materialstock.repository.StockQueryRepository
import com.lumos.lumosspring.stock.order.teamrequest.model.OrderMaterial
import com.lumos.lumosspring.stock.order.teamrequest.model.OrderMaterialItem
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialItemRepository
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialRepository
import com.lumos.lumosspring.team.repository.TeamQueryRepository
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class MaterialStockRegisterService(
    private val orderMaterialRepository: OrderMaterialRepository,
    private val orderMaterialItemRepository: OrderMaterialItemRepository,
    private val teamQueryRepository: TeamQueryRepository,
) {
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

        if (exists) {
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