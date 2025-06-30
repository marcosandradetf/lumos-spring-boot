package com.lumos.lumosspring.reservation.service

import com.lumos.lumosspring.reservation.controller.ReservationController
import com.lumos.lumosspring.reservation.controller.ReservationController.ReserveItem
import com.lumos.lumosspring.util.JdbcUtil
import com.lumos.lumosspring.util.ReservationStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationService(
    private val namedJdbc: NamedParameterJdbcTemplate
) {

    @Transactional
    fun reply(replies: ReservationController.Replies): ResponseEntity<Void> {
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


            if (replies.approved.contains(ReserveItem(reservationId))) {
                if (status == ReservationStatus.PENDING)
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
            } else if (replies.rejected.contains(ReserveItem(reservationId))) {
                if (status == ReservationStatus.PENDING) {
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
                            UPDATE material_stock set stock_available = stock_available + :reserveQuantity
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


        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    fun markAsCollected(replies: ReservationController.Replies): ResponseEntity<Void> {
        val reservationIds = replies.approved.map { it.reserveId } + replies.rejected.map { it.reserveId }

        val reservations = JdbcUtil.getRawData(
            namedJdbc,
            """
                    SELECT material_id_reservation, central_material_stock_id, 
                    truck_material_stock_id, status, direct_execution_id, pre_measurement_street_id
                    FROM material_reservation
                    WHERE material_id_reservation in (:reservationIds)
                """.trimIndent(),
            mapOf("reservationIds" to reservationIds)
        )

        val reservationsGroup = reservations.groupBy {
            when {
                it["direct_execution_id"] != null -> "direct" to it["direct_execution_id"] as Long
                it["pre_measurement_street_id"] != null -> "pre" to it["pre_measurement_street_id"] as Long
                else -> throw IllegalArgumentException("Execuções não encontradas")
            }
        }


        for ((key, reservation) in reservationsGroup) {
            val (type, id) = key

            for (reservation in reservations) {
                val reservationId = reservation["material_id_reservation"] as Long
                val centralMaterialId = reservation["central_material_stock_id"] as Long
                val truckMaterialId = reservation["truck_material_stock_id"] as Long
                val reserveQuantity = reservation["truck_material_stock_id"] as Long

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

                    namedJdbc.update(
                        """
                            UPDATE material_stock set stock_quantity = stock_quantity - :reserveQuantity
                            WHERE material_id_stock in (:centralMaterialId)
                        """.trimIndent(),
                        mapOf(
                            "centralMaterialId" to centralMaterialId,
                            "reserveQuantity" to reserveQuantity
                        )
                    )

                } else if (replies.rejected.contains(ReserveItem(reservationId))) {

                }
            }

            if (type == "direct") {

            } else {

            }

        }


        return ResponseEntity(HttpStatus.NO_CONTENT)
    }


}