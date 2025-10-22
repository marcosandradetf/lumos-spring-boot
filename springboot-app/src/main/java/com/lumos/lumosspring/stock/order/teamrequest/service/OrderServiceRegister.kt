package com.lumos.lumosspring.stock.order.teamrequest.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.stock.order.teamrequest.dto.ReplyRequest
import com.lumos.lumosspring.stock.order.teamrequest.dto.OrderRequest
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil
import com.lumos.lumosspring.util.JdbcUtil.getRawData
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class OrderServiceRegister(
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val notificationService: NotificationService,
    private val orderMaterialRepository: OrderMaterialRepository,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    @Transactional
    fun reply(replyRequest: ReplyRequest): ResponseEntity<Void> {
        val reservationIds = replyRequest.approved.map { it.reserveId } + replyRequest.rejected.map { it.reserveId }
        val orders = replyRequest.approved.map { it.order } + replyRequest.rejected.map { it.order }

        val reservations = getRawData(
            namedJdbc,
            """
                    SELECT mr.material_id_reservation, mr.status, mr.reserved_quantity, mr.central_material_stock_id,
                    COALESCE(de.reservation_management_id, pm.reservation_management_id) AS reservation_management_id,
                    mr.direct_execution_id, mr.direct_execution_id, mr.contract_item_id
                    FROM material_reservation mr
                    LEFT JOIN pre_measurement pm on pm.pre_measurement_id = mr.pre_measurement_id
                    LEFT JOIN direct_execution de ON de.direct_execution_id = mr.direct_execution_id
                    WHERE material_id_reservation in (:reservationIds)
                """.trimIndent(),
            mapOf("reservationIds" to reservationIds)
        )

        for (reservation in reservations) {
            val reservationId = reservation["material_id_reservation"] as Long
            val reservationManagementId = reservation["reservation_management_id"] as? Long
            val status = reservation["status"] as String
            val reserveQuantity = reservation["reserved_quantity"] as Double
            val centralMaterialId = reservation["central_material_stock_id"] as Long

            val directExecutionId = reservation["direct_execution_id"] as? Long
            val contractItemId = reservation["contract_item_id"] as Long

            if (status != ReservationStatus.PENDING) continue

            if (replyRequest.approved.contains(OrderRequest(reservationId))) {
                namedJdbc.update(
                    """
                            UPDATE material_reservation set status = :status
                            WHERE material_id_reservation in (:reservationId)
                        """.trimIndent(),
                    mapOf(
                        "reservationId" to reservationId,
                        "status" to ReservationStatus.APPROVED
                    )
                )

//                TODO("IMPLEMENTAR ENVIO DE NOTIFICAÇÃO")
            } else if (replyRequest.rejected.contains(OrderRequest(reservationId))) {

                namedJdbc.update(
                    """
                            UPDATE material_reservation set status = :status
                            WHERE material_id_reservation in (:reservationId)
                        """.trimIndent(),
                    mapOf(
                        "reservationId" to reservationId,
                        "status" to ReservationStatus.REJECTED
                    )
                )

                namedJdbc.update(
                    """
                            UPDATE material_stock 
                            set stock_available = stock_available + :reserveQuantity
                            WHERE material_id_stock = :centralMaterialId
                        """.trimIndent(),
                    mapOf(
                        "centralMaterialId" to centralMaterialId,
                        "reserveQuantity" to reserveQuantity
                    )
                )

                namedJdbc.update(
                    """
                            UPDATE reservation_management set status = :status
                            WHERE reservation_management_id = :reservationManagementId
                        """.trimIndent(),
                    mapOf(
                        "reservationManagementId" to reservationManagementId,
                        "status" to ReservationStatus.PENDING
                    )
                )

                if (directExecutionId != null) {
                    namedJdbc.update(
                        """
                                UPDATE direct_execution_item set item_status = :status
                                WHERE contract_item_id = :contractItemId
                            """.trimIndent(),
                        mapOf(
                            "contractItemId" to contractItemId,
                            "status" to ReservationStatus.PENDING
                        )
                    )
                } else {
                    namedJdbc.update(
                        """
                                UPDATE pre_measurement_street_item set item_status = :status
                                WHERE contract_item_id = :contractItemId
                            """.trimIndent(),
                        mapOf(
                            "contractItemId" to contractItemId,
                            "status" to ReservationStatus.PENDING
                        )
                    )
                }
            }
        }


        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @Transactional
    fun markAsCollected(orders: List<OrderRequest>): ResponseEntity<Void> {
        if (orders.isEmpty()) throw IllegalStateException("Nenhuma reserva foi enviada")

        data class Quadruple<A, B, C, D>(
            val first: A,
            val second: B,
            val third: C,
            val fourth: D
        )

        val destination = mutableSetOf(Quadruple("", "", "", 0L))
        val ordersJson = objectMapper.writeValueAsString(
            orders.map { mapOf("order_id" to it.order.orderId, "material_id" to it.order.materialId) }
        )

        val ordersByKeys = orderMaterialRepository.getOrdersByKeys(
            orders.map { it.reserveId ?: -1L },
            ordersJson
        )

        for (o in ordersByKeys) {
            if (o.status == ReservationStatus.COLLECTED) continue

            if (o.directExecutionId != null) {
                destination.add(
                    Quadruple("direct_execution", "direct_execution_status", "direct_execution_id", o.directExecutionId)
                )
            } else if (o.preMeasurementId != null) {
                destination.add(
                    Quadruple("pre_measurement", "status", "pre_measurement_id", o.preMeasurementId)
                )
            }

            namedJdbc.update(
                """
                        UPDATE material_stock 
                        set stock_quantity = stock_quantity + :reserveQuantity,
                            stock_available = stock_available + :reserveQuantity
                        where material_id_stock = :truckMaterialId
                    """.trimIndent(),
                mapOf(
                    "reserveQuantity" to o.requestQuantity,
                    "truckMaterialId" to o.truckMaterialStockId,
                )
            )

            namedJdbc.update(
                """
                        UPDATE material_stock 
                        set stock_quantity = stock_quantity - :reserveQuantity
                        where material_id_stock = :centralMaterialId
                    """.trimIndent(),
                mapOf(
                    "reserveQuantity" to o.requestQuantity,
                    "centralMaterialId" to o.truckMaterialStockId,
                )
            )

            // ->
            // corrigir quantidade, status
            // <-

            val (sql, param) = if (o.materialIdReservation != null) {
                """
                    UPDATE material_reservation 
                    SET status = :status
                    WHERE material_id_reservation = :reservationId
                """.trimIndent() to mapOf(
                    "reservationId" to o.materialIdReservation,
                    "status" to ReservationStatus.COLLECTED
                )
            } else {
                """
                    UPDATE order_material_item 
                    SET status = :status
                    WHERE material_id = :materialId
                        AND order_id = :orderId
                """.trimIndent() to mapOf(
                    "materialId" to o.materialId,
                    "orderId" to o.orderId,
                    "status" to ReservationStatus.COLLECTED
                )
            }

            namedJdbc.update(
                sql,
                param
            )
        }

        val (tableName, statusName, keyName, keyId) = destination
        val statusReservationsData = getRawData(
            namedJdbc,
            """
                    SELECT 1
                    FROM material_reservation
                    WHERE :keyName = :keyId AND status <> :status
                """.trimIndent(),
            mapOf(
                "keyName" to keyName,
                "keyId" to keyId,
                "status" to ReservationStatus.COLLECTED,
            )
        )

        if (statusReservationsData.isEmpty()) {
            namedJdbc.update(
                """
                update :tableName
                set :statusName = :status
                where :keyName = :keyId
            """.trimIndent(),
                mapOf(
                    "tableName" to tableName,
                    "statusName" to statusName,
                    "keyName" to keyName,
                    "keyId" to keyId,
                    "status" to ExecutionStatus.AVAILABLE_EXECUTION
                )
            )
        }

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }


}