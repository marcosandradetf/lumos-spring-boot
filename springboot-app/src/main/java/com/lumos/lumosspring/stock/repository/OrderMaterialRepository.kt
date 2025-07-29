package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.dto.order.DataForRequisition
import com.lumos.lumosspring.dto.reservation.OrderItemRequest
import com.lumos.lumosspring.stock.entities.OrderMaterial
import com.lumos.lumosspring.stock.entities.OrderMaterialItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderMaterialRepository : CrudRepository<OrderMaterial, UUID> {
    @Query("""
        select deposit_id as depositId, status, team_id as teamId
        from order_material
        where order_id in (:orderId)
    """)
    fun getDataForRequisition(orderId: List<UUID>): List<DataForRequisition>
}

@Repository
interface OrderMaterialItemRepository : CrudRepository<OrderMaterialItem, UUID> {
    @Query("""
        select material_id 
        from order_material_item
        where order_id = :orderId
    """)
    fun getMaterialsIdsByOrder(orderId: UUID): List<Long>
}