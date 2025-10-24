package com.lumos.lumosspring.stock.order.teamrequest.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.stock.order.teamrequest.dto.ReplyRequest
import com.lumos.lumosspring.stock.order.teamrequest.dto.OrderRequest
import com.lumos.lumosspring.stock.order.teamrequest.repository.OrderMaterialRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil.getRawData
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderServiceRegister(
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val notificationService: NotificationService,
    private val orderMaterialRepository: OrderMaterialRepository,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    @Transactional
    fun reply(orders: ReplyRequest): ResponseEntity<Void> {
        val reservationsJson = objectMapper.writeValueAsString(
            orders.approved.map {
                mapOf(
                    "reservation_id" to it.reserveId,
                    "status" to "APPROVED"
                )
            } + orders.rejected.map {
                mapOf(
                    "reservation_id" to it.reserveId,
                    "status" to "REJECTED"
                )
            }
        )

        val ordersJson = objectMapper.writeValueAsString(
            orders.approved.map {
                mapOf(
                    "order_id" to it.order.orderId,
                    "material_id" to it.order.materialId,
                    "quantity" to it.order.quantity,
                    "status" to "APPROVED"
                )
            } + orders.rejected.map {
                mapOf(
                    "order_id" to it.order.orderId,
                    "material_id" to it.order.materialId,
                    "quantity" to null,
                    "status" to "REJECTED"
                )
            }
        )

        if (ordersJson.trim() != "[]") {
            namedJdbc.update(
                """
                        UPDATE order_material_item omi
                        SET status = data.status,
                            quantity_released = data.quantity
                        FROM jsonb_to_recordset(:ordersJson::jsonb) AS data(
                            order_id uuid,
                            material_id bigint,
                            quantity numeric,
                            status text
                        )
                        WHERE omi.order_id = data.order_id
                          AND omi.material_id = data.material_id
                    """.trimIndent(),
                mapOf("ordersJson" to ordersJson)
            )
        }

        if (reservationsJson.trim() != "[]") {
            namedJdbc.query(
                """
                        UPDATE material_reservation mr
                        SET status = data.status
                        FROM jsonb_to_recordset(:reservationsJson::jsonb) AS data(
                            reservation_id bigint,
                            status text
                        ), team t
                        WHERE mr.material_id_reservation = data.reservation_id
                            AND t.id_team = mr.team_id
                        RETURNING mr.direct_execution_id, mr.pre_measurement_id, mr.contract_item_id, data.status, t.notification_code
                    """.trimIndent(),
                mapOf("reservationsJson" to reservationsJson)
            ) { rs, _ ->
                val status = rs.getString("status")
                val directExecutionId = rs.getLong("direct_execution_id").let { if (rs.wasNull()) null else it }
                val preMeasurementId = rs.getLong("pre_measurement_id").let { if (rs.wasNull()) null else it }
                val contractItemId = rs.getLong("contract_item_id")
                val notificationCode = rs.getString("notification_code")

                if (status == "APPROVED") {
                    notificationService.sendNotificationForTopic(
                        title = TODO(),
                        body = TODO(),
                        action = TODO(),
                        notificationCode = notificationCode,
                        time = TODO(),
                        type = TODO()
                    )
                } else {
                    if (directExecutionId != null) {
                        namedJdbc.update(
                            """
                            WITH updated AS (
                                UPDATE direct_execution_item dei
                                SET item_status = :status
                                FROM direct_execution de
                                WHERE dei.contract_item_id = :contractItemId and de.direct_execution_id = dei.direct_execution_id
                                RETURNING de.reservation_management_id
                            )
                            UPDATE reservation_management 
                            SET status = :status
                            WHERE reservation_management_id IN (SELECT reservation_management_id FROM updated);
                        """.trimIndent(),
                            mapOf(
                                "contractItemId" to contractItemId,
                                "status" to ReservationStatus.PENDING
                            )
                        )
                    } else {
                        namedJdbc.update(
                            """
                            WITH updated AS (
                                UPDATE pre_measurement_street_item psi
                                SET item_status = :status
                                FROM pre_measurement p
                                WHERE psi.contract_item_id = :contractItemId
                                    AND psi.pre_measurement_id = :preMeasurementId
                                    AND p.pre_measurement_id = psi.pre_measurement_id
                                RETURNING p.reservation_management_id
                            )
                            UPDATE reservation_management 
                            SET status = :status
                            WHERE reservation_management_id IN (SELECT reservation_management_id FROM updated);
                        """.trimIndent(),
                            mapOf(
                                "contractItemId" to contractItemId,
                                "preMeasurementId" to preMeasurementId,
                                "status" to ReservationStatus.PENDING
                            )
                        )
                    }
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

        val destinations = mutableSetOf(Quadruple("", "", "", 0L))
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
                destinations.add(
                    Quadruple("direct_execution", "direct_execution_status", "direct_execution_id", o.directExecutionId)
                )
            } else if (o.preMeasurementId != null) {
                destinations.add(
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

            val (sql, param) = if (o.materialIdReservation != null || o.directExecutionId != null) {
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

        for (destination in destinations) {
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
        }

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }


}