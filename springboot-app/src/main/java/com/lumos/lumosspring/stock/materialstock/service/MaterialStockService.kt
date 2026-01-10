package com.lumos.lumosspring.stock.materialstock.service


import com.lumos.lumosspring.stock.order.teamrequest.model.OrderMaterial
import com.lumos.lumosspring.stock.order.teamrequest.model.OrderMaterialItem
import com.lumos.lumosspring.stock.deposit.repository.DepositRepository
import com.lumos.lumosspring.stock.materialstock.dto.MaterialInStockDTO
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockViewRepository
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialItemRepository
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialRepository
import com.lumos.lumosspring.stock.materialstock.repository.StockQueryRepository
import com.lumos.lumosspring.team.repository.TeamQueryRepository
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class MaterialStockService(
    private val stockQueryRepository: StockQueryRepository,
    private val orderMaterialRepository: OrderMaterialRepository,
    private val orderMaterialItemRepository: OrderMaterialItemRepository,
    private val teamQueryRepository: TeamQueryRepository,
    private val depositRepository: DepositRepository,
    private val materialStockViewRepository: MaterialStockViewRepository,
) {

    fun getStockMaterialForLinking(linking: String, type: String, teamId: Long): ResponseEntity<Any> {
        val materials: List<MaterialInStockDTO> = if (type != "NULL" && linking != "NULL") {
            materialStockViewRepository.findAllByLinkingAndType(
                linking.lowercase(),
                type.lowercase(),
                teamId
            )
        } else {
            materialStockViewRepository.findAllByType(type.lowercase(), teamId)
        }

        return ResponseEntity.ok(materials)
    }

    fun getMaterialsForMaintenance(
        userId: UUID,
        currentTeamId: Long? = null
    ): ResponseEntity<Any> {
        val teamId =
            currentTeamId ?: (teamQueryRepository.getTeamIdByUserId(userId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Equipe enviada não encontrada, informe ao Administrador do sistema"))

        val depositId: Long? =
            depositRepository.getDepositIdByTeamId(teamId) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Depósito enviado não encontrado, informe ao Administrador do sistema")

        return if (depositId != null) {
            ResponseEntity.ok(stockQueryRepository.getMaterialsForMaintenance(depositId))
        } else {
            ResponseEntity.notFound().build()
        }
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