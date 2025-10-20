package com.lumos.lumosspring.stock.order.service

import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.stock.order.dto.Replies
import com.lumos.lumosspring.stock.order.dto.RepliesReserves
import com.lumos.lumosspring.stock.order.dto.ReserveItem
import com.lumos.lumosspring.stock.order.repository.OrderMaterialRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil
import com.lumos.lumosspring.util.JdbcUtil.getRawData
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OrderService(
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val notificationService: NotificationService,
    private val orderMaterialRepository: OrderMaterialRepository
) {

    fun getReservationsByStatusAndStockist(depositId: Long, status: String): ResponseEntity<Any> {
        data class OrderDto(
            val reserveId: Long?,
            val materialId: Long?,
            val orderId: String?,

            val reserveQuantity: Double?,
            val stockQuantity: Double,
            val materialName: String,
            val description: String?,
            val status: String,
        )

        data class OrdersByCaseResponse(
            val description: String,
            val teamName: String?,
            val reservations: List<OrderDto>
        )

        val response: MutableList<OrdersByCaseResponse> = mutableListOf()

        val rawReservations = getRawData(
            namedJdbc,
            """
                -- Reservas para instalações
                select pms.city, c.contractor, mr.material_id_reservation, 
                cast(null as uuid) as order_id, mr.reserved_quantity as request_quantity, 
                mr.description, m.id_material, m.material_name, 
                m.material_power, m.material_length, t.team_name,
                ms.stock_quantity, mr.status, cast(null as timestamp) as created_at
                from material_reservation mr
                inner join material_stock ms on ms.material_id_stock = mr.central_material_stock_id
                inner join material m on m.id_material = ms.material_id
                left join direct_execution de on mr.direct_execution_id = de.direct_execution_id
                left join pre_measurement pm on pm.pre_measurement_id = mr.pre_measurement_id
                inner join team t on t.id_team = mr.team_id
                left join contract c on de.contract_id = c.contract_id
                where ms.deposit_id = :deposit_id and mr.status = :status

                UNION ALL

                -- Pedidos da equipe
                select cast(null as text) as city, cast(null as text) as contractor, cast(null as bigint) as material_id_reservation, om.order_id,
                cast(null as bigint) as request_quantity, om.order_code as description, m.id_material,
                m.material_name, m.material_power, m.material_length, t.team_name,
                ms.stock_quantity, om.status, om.created_at
                from order_material om
                inner join order_material_item omi on omi.order_id = om.order_id
                inner join material_stock ms on ms.material_id = omi.material_id
                inner join material m on m.id_material = ms.material_id
                inner join team t on t.id_team = om.team_id
                where ms.deposit_id = :deposit_id and om.status = :status and ms.deposit_id = om.deposit_id

                order by created_at nulls last, material_id_reservation nulls last;
            """.trimIndent(),
            mapOf("deposit_id" to depositId, "status" to status)
        )

        val reservationsGroup = rawReservations
            .groupBy {
                (it["city"] as? String) ?: (it["contractor"] as? String) ?: (it["description"] as? String)
                ?: "Desconhecido"
            }

        for ((preMeasurementName, reservations) in reservationsGroup) {
            val list = mutableListOf<OrderDto>()
            for (reserve in reservations) {
                var materialName = (reserve["material_name"] as String)
                val power: String? = (reserve["material_power"] as? String)
                val length: String? = (reserve["material_length"] as? String)
                if (power != null) materialName += " $power"
                else if (length != null) materialName += " $length"

                list.add(
                    OrderDto(
                        reserveId = (reserve["material_id_reservation"] as Number?)?.toLong(),
                        orderId = (reserve["order_id"] as? UUID)?.toString(),

                        reserveQuantity = (reserve["request_quantity"] as? Number)?.toDouble(),
                        stockQuantity = (reserve["stock_quantity"] as Number).toDouble(),
                        materialId = reserve["id_material"] as Long,
                        materialName = materialName,
                        description = (reserve["description"] as? String),
                        status = reserve["status"] as String,
                    )
                )
            }

            response.add(
                OrdersByCaseResponse(
                    description = preMeasurementName,
                    teamName = reservations.first()["team_name"] as? String,
                    reservations = list
                )
            )
        }


        return ResponseEntity.ok().body(response)
    }


    @Transactional
    fun reply(replies: Replies): ResponseEntity<Void> {
        replyReservations(RepliesReserves(replies.approvedReserves, replies.rejectedReserves))
//        replyOrders(RepliesOrders(replies.approvedOrders, replies.rejectedOrders))

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @Transactional fun replyReservations(replies: RepliesReserves) {
        val reservationIds = replies.approved.map { it.reserveId } + replies.rejected.map { it.reserveId }

        val reservations = JdbcUtil.getRawData(
            namedJdbc,
            """
                    SELECT mr.material_id_reservation, mr.status, mr.reserved_quantity, mr.central_material_stock_id,
                    COALESCE(de.reservation_management_id, pms.reservation_management_id) AS reservation_management_id,
                    mr.direct_execution_id, mr.direct_execution_id, mr.contract_item_id
                    FROM material_reservation mr
                    LEFT JOIN pre_measurement_street pms on pms.pre_measurement_street_id = mr.pre_measurement_street_id
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

            if (replies.approved.contains(ReserveItem(reservationId))) {
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
            } else if (replies.rejected.contains(ReserveItem(reservationId))) {

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
    }

//    @Transactional fun replyOrders(replies:  RepliesOrders) {
//        val orderIds = listOf(replies.approved.orderId, replies.rejected.orderId)
//        val orders = orderMaterialRepository.getDataForRequisition(orderIds)
//        val orderItems = replies.approved.orderItemRequests + replies.rejected.orderItemRequests
//
//        for (order in orders) {
//            if (order.status != ReservationStatus.PENDING) continue
//
//            for(item in orderItems) {
//                if (replies.approved.orderItemRequests.contains(OrderItemRequest(item.materialId, item.quantity))) {
//                    namedJdbc.update(
//                        """
//                            UPDATE order_material_item set status = :status
//                            WHERE order_id = :orderId and material_id = :materialId
//                        """.trimIndent(),
//                        mapOf(
//                            "orderId" to order.orderId,
//                            "materialId" to item.materialId,
//                            "status" to ReservationStatus.APPROVED
//                        )
//                    )
//
////                TODO("IMPLEMENTAR ENVIO DE NOTIFICAÇÃO")
//                } else if (replies.rejected.contains(ReserveItem(reservationId))) {
//
//                    namedJdbc.update(
//                        """
//                            UPDATE material_reservation set status = :status
//                            WHERE material_id_reservation in (:reservationId)
//                        """.trimIndent(),
//                        mapOf(
//                            "reservationId" to reservationId,
//                            "status" to ReservationStatus.REJECTED
//                        )
//                    )
//
//                    namedJdbc.update(
//                        """
//                            UPDATE material_stock set stock_available = stock_available + :reserveQuantity
//                            WHERE material_id_stock = :centralMaterialId
//                        """.trimIndent(),
//                        mapOf(
//                            "centralMaterialId" to centralMaterialId,
//                            "reserveQuantity" to reserveQuantity
//                        )
//                    )
//
//                    namedJdbc.update(
//                        """
//                            UPDATE reservation_management set status = :status
//                            WHERE reservation_management_id = :reservationManagementId
//                        """.trimIndent(),
//                        mapOf(
//                            "reservationManagementId" to reservationManagementId,
//                            "status" to ReservationStatus.PENDING
//                        )
//                    )
//
//                    if (directExecutionId != null) {
//                        namedJdbc.update(
//                            """
//                                UPDATE direct_execution_item set item_status = :status
//                                WHERE contract_item_id = :contractItemId
//                            """.trimIndent(),
//                            mapOf(
//                                "contractItemId" to contractItemId,
//                                "status" to ReservationStatus.PENDING
//                            )
//                        )
//                    } else {
//                        namedJdbc.update(
//                            """
//                                UPDATE pre_measurement_street_item set item_status = :status
//                                WHERE contract_item_id = :contractItemId
//                            """.trimIndent(),
//                            mapOf(
//                                "contractItemId" to contractItemId,
//                                "status" to ReservationStatus.PENDING
//                            )
//                        )
//                    }
//                }
//            }
//
//
//        }
//    }

    @Transactional
    fun markAsCollected(reservationIds: List<Long>): ResponseEntity<Void> {
        if (reservationIds.isEmpty()) throw IllegalStateException("Nenhuma reserva foi enviada")

        data class Quadruple<A, B, C, D>(
            val first: A,
            val second: B,
            val third: C,
            val fourth: D
        )

        var destination: Quadruple<String, String, String, Long> = Quadruple("", "", "", 0)

        val reservations = JdbcUtil.getRawData(
            namedJdbc,
            """
                    SELECT material_id_reservation, central_material_stock_id, reserved_quantity,
                    direct_execution_id, pre_measurement_street_id, status, truck_material_stock_id
                    FROM material_reservation
                    WHERE material_id_reservation in (:reservationIds)
                """.trimIndent(),
            mapOf("reservationIds" to reservationIds)
        )

        for (r in reservations) {
            val (tableName, statusName, keyName, keyId) = destination

            val reservationId = r["material_id_reservation"] as Long
            val centralMaterialId = r["central_material_stock_id"] as Long
            val truckMaterialId = r["truck_material_stock_id"] as Long
            val reserveQuantity = r["reserved_quantity"] as Long
            val directExecutionId = r["direct_execution_id"] as? Long
            val streetId = r["pre_measurement_street_id"] as? Long

            if (r["status"] == ReservationStatus.COLLECTED) continue

            if (
                (tableName == "direct_execution" && keyId != directExecutionId) ||
                (tableName == "pre_measurement_street" && keyId != streetId)
            ) {
                throw IllegalStateException("Reservation Service - Mais de uma execução encontrada")
            } else if (tableName.isBlank()) {
                destination = if (directExecutionId != null) {
                    Quadruple("direct_execution", "direct_execution_status", "direct_execution_id", directExecutionId)
                } else if (streetId != null) {
                    Quadruple("pre_measurement_street", "street_status", "pre_measurement_street_id", streetId)
                } else {
                    throw IllegalStateException("Reservation Service - Nenhuma execução foi encontrada")
                }
            }

            namedJdbc.update(
                """
                        UPDATE material_stock 
                        set stock_quantity = stock_quantity + :reserveQuantity,
                            stock_available = stock_available + :reserveQuantity
                        where material_id_stock = :truckMaterialId
                    """.trimIndent(),
                mapOf(
                    "reserveQuantity" to reserveQuantity,
                    "truckMaterialId" to truckMaterialId,
                )
            )

            namedJdbc.update(
                """
                        UPDATE material_reservation set status = :status
                        where material_id_reservation in (:reservationId)
                    """.trimIndent(),
                mapOf(
                    "reservationId" to reservationId,
                    "status" to ReservationStatus.COLLECTED
                )
            )

            namedJdbc.update(
                """
                        UPDATE material_stock 
                        set stock_quantity = stock_quantity - :reserveQuantity
                        where material_id_stock = :centralMaterialId
                    """.trimIndent(),
                mapOf(
                    "reserveQuantity" to reserveQuantity,
                    "centralMaterialId" to centralMaterialId,
                )
            )

        }

        val (tableName, statusName, keyName, keyId) = destination
        val statusReservationsData = JdbcUtil.getRawData(
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