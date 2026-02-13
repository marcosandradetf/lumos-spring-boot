package com.lumos.lumosspring.stock.order.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.stock.order.dto.ReplyRequest
import com.lumos.lumosspring.stock.order.dto.OrderRequest
import com.lumos.lumosspring.stock.order.repository.OrderMaterialRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil.getRawData
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.ReservationStatus
import com.lumos.lumosspring.util.Utils
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
            val updatedOrders = mutableSetOf<Pair<String, String>>()
            namedJdbc.query(
                """
                        UPDATE order_material_item omi
                        SET status = data.status,
                            quantity_released = data.quantity
                        FROM jsonb_to_recordset(:ordersJson::jsonb) AS data(
                            order_id uuid,
                            material_id bigint,
                            quantity numeric,
                            status text
                        ), order_material o, team t
                        WHERE omi.order_id = data.order_id
                          AND omi.material_id = data.material_id
                          AND o.order_id = data.order_id
                          AND t.id_team = o.team_id
                        RETURNING t.notification_code, o.order_code
                    """.trimIndent(),
                mapOf("ordersJson" to ordersJson)
            ) { rs, _ ->
                val notificationCode = rs.getString("notification_code")
                val orderCode = rs.getString("order_code")
                updatedOrders.add(Pair(notificationCode, orderCode))
            }

            updatedOrders.forEach {
                notificationService.sendNotificationForTopic(
                    title = "Atualização na sua solicitação",
                    body = "O status da sua requisição de materiais ${it.second} foi atualizado. Toque para ver os detalhes.",
                    action = "APPROVATED_ORDERS",
                    notificationCode = it.first,
                    type = NotificationType.ALERT
                )
            }
        }

        if (reservationsJson.trim() != "[]") {
            val notificationCodes = mutableSetOf<Triple<String, String, String>>()
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
                        RETURNING mr.direct_execution_id, mr.pre_measurement_id, mr.contract_item_id, data.status, t.notification_code, mr.description
                    """.trimIndent(),
                mapOf("reservationsJson" to reservationsJson)
            ) { rs, _ ->
                val status = rs.getString("status")
                val description = rs.getString("description")
                val teamNotificationCode = rs.getString("notification_code")
                val directExecutionId = rs.getLong("direct_execution_id").let { if (rs.wasNull()) null else it }
                val preMeasurementId = rs.getLong("pre_measurement_id").let { if (rs.wasNull()) null else it }
                val contractItemId = rs.getLong("contract_item_id")

                if (status != "APPROVED") {
                    if (directExecutionId != null) {
                        namedJdbc.query(
                            """
                            WITH updated AS (
                                UPDATE direct_execution_item dei
                                SET item_status = :status
                                FROM direct_execution de
                                WHERE dei.contract_item_id = :contractItemId 
                                    AND dei.direct_execution_id = :directExecutionId
                                    AND de.direct_execution_id = :directExecutionId
                                RETURNING de.reservation_management_id
                            )
                            UPDATE reservation_management
                            SET status = :status
                            WHERE reservation_management_id IN (SELECT reservation_management_id FROM updated)
                                AND status <> :status
                            RETURNING stockist_id, description;
                        """.trimIndent(),
                            mapOf(
                                "contractItemId" to contractItemId,
                                "directExecutionId" to directExecutionId,
                                "status" to ReservationStatus.PENDING
                            )
                        ) { rs, _ ->
                            notificationCodes
                                .add(
                                    Triple(
                                        "REJECTED",
                                        rs.getString("stockist_id"),
                                        rs.getString("description"),
                                    )
                                )
                        }
                    } else {
                        namedJdbc.query(
                            """
                            WITH updated AS (
                                UPDATE pre_measurement_street_item psi
                                SET item_status = :status
                                FROM pre_measurement p
                                WHERE psi.contract_item_id = :contractItemId
                                    AND psi.pre_measurement_id = :preMeasurementId
                                    AND p.pre_measurement_id = :preMeasurementId
                                RETURNING p.reservation_management_id
                            )
                            UPDATE reservation_management 
                            SET status = :status
                            WHERE reservation_management_id IN (SELECT reservation_management_id FROM updated)
                                AND status <> :status
                            RETURNING stockist_id, description;
                        """.trimIndent(),
                            mapOf(
                                "contractItemId" to contractItemId,
                                "preMeasurementId" to preMeasurementId,
                                "status" to ReservationStatus.PENDING
                            )
                        ) { rs, _ ->
                            notificationCodes
                                .add(
                                    Triple(
                                        "REJECTED",
                                        rs.getString("stockist_id"),
                                        rs.getString("description"),
                                    )
                                )
                        }
                    }
                } else {
                    notificationCodes
                        .add(
                            Triple(
                                "APPROVED",
                                teamNotificationCode,
                                description,
                            )
                        )

                }

                notificationCodes.forEach {
                    if (it.first === "APPROVED") {
                        notificationService.sendNotificationForTopic(
                            title = "Materiais prontos para instalação",
                            body = "Os materiais para **${it.third}** estão disponíveis no almoxarifado. Toque para ver os detalhes.",
                            action = "APPROVATED_ORDERS",
                            notificationCode = it.second,
                            type = NotificationType.ALERT
                        )
                    } else {
                        notificationService.sendNotificationForTopic(
                            title = "Gerenciamento pendente de materiais",
                            body = "Alguns materiais da instalação **${it.third}** foram recusados pelo estoquista. Refaça o gerenciamento para prosseguir com a instalação.",
                            action = "/requisicoes/instalacoes/gerenciamento-estoque",
                            notificationCode = it.second,
                            type = NotificationType.ALERT
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

        val destinations = mutableSetOf<Quadruple<String, String, String, Long>>()
        val ordersJson = objectMapper.writeValueAsString(
            orders.map { mapOf("order_id" to it.order.orderId, "material_id" to it.order.materialId) }
        )

        // o erro esta aqui, a query nao retorna nada
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

            val updated: Int? = namedJdbc.queryForObject(
                """
                        WITH current_stock AS (
                            UPDATE material_stock ms
                            SET stock_quantity = ms.stock_quantity - :requestQuantity,
                                stock_available = ms.stock_available - :requestQuantity
                            WHERE ms.material_id_stock = :centralMaterialStockId
                              AND ms.stock_quantity >= :requestQuantity
                            RETURNING 1
                        ),
                        updated_truck AS (
                            UPDATE material_stock tms
                            SET stock_quantity = tms.stock_quantity + :requestQuantity,
                                stock_available = tms.stock_available + :requestQuantity
                            WHERE tms.material_id_stock = :truckMaterialStockId
                              AND EXISTS (SELECT 1 FROM current_stock)
                            RETURNING 1
                        )
                        SELECT * FROM updated_truck;
                    """.trimIndent(),
                mapOf(
                    "requestQuantity" to o.requestQuantity,
                    "truckMaterialId" to o.truckMaterialStockId,
                    "centralMaterialId" to o.centralMaterialStockId,
                ), Int::class.java
            )

            if(updated == null) {
                throw Utils.BusinessException("O material ${o.materialName} não possui estoque suficiente.")
            }

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

            // Monta dinamicamente o SQL com os nomes de coluna e tabela
            val sqlSelect = """
                SELECT 1
                FROM material_reservation
                WHERE $keyName = :keyId AND status <> :status
            """.trimIndent()

            val statusReservationsData = getRawData(
                namedJdbc,
                sqlSelect,
                mapOf(
                    "keyId" to keyId,
                    "status" to ReservationStatus.COLLECTED,
                )
            )

            if (statusReservationsData.isEmpty()) {
                val sqlUpdate = """
                    UPDATE $tableName
                    SET $statusName = :status
                    WHERE $keyName = :keyId
                """.trimIndent()

                namedJdbc.update(
                    sqlUpdate,
                    mapOf(
                        "keyId" to keyId,
                        "status" to ExecutionStatus.AVAILABLE_EXECUTION
                    )
                )
            }
        }


        return ResponseEntity(HttpStatus.NO_CONTENT)
    }


}