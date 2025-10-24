package com.lumos.lumosspring.stock.order.teamrequest.repository

import com.lumos.lumosspring.stock.order.teamrequest.dto.OrdersByKeysView
import com.lumos.lumosspring.stock.order.teamrequest.dto.OrdersByStatusView
import com.lumos.lumosspring.stock.order.teamrequest.model.OrderMaterial
import com.lumos.lumosspring.stock.order.teamrequest.model.OrderMaterialItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderMaterialRepository : CrudRepository<OrderMaterial, UUID> {


    @Query("""
        -- Installation requests
        select 
            coalesce(cd.contractor, pm.city) as contractor, 
            mr.material_id_reservation as materialIdReservation, 
            cast(null as uuid) as orderId, 
            mr.reserved_quantity as requestQuantity, 
            mr.description, 
            m.id_material as materialId, 
            m.material_name || ' ' || coalesce(m.material_power, m.material_length) as materialName, 
            t.team_name as teamName,
            ms.stock_quantity as stockQuantity, 
            mr.status, 
            cast(null as timestamp) as createdAt
        from material_reservation mr
        join material_stock ms on ms.material_id_stock = mr.central_material_stock_id
        join material m on m.id_material = ms.material_id
        left join direct_execution de on mr.direct_execution_id = de.direct_execution_id
        left join pre_measurement pm on pm.pre_measurement_id = mr.pre_measurement_id
        join team t on t.id_team = mr.team_id
        left join contract cd on de.contract_id = cd.contract_id
        where ms.deposit_id = :deposit_id and mr.status = :status

        UNION ALL

        -- Team requests
        select 
            cast(null as text) as contractor, 
            cast(null as bigint) as materialIdReservation, 
            om.order_id as orderId,
            cast(null as bigint) as requestQuantity, 
            om.order_code as description, 
            m.id_material as materialId,
            m.material_name || ' ' || coalesce(m.material_power, m.material_length) as materialName,
            t.team_name as teamName,
            ms.stock_quantity as stockQuantity, 
            omi.status, 
            om.created_at as createdAt
        from order_material om
        join order_material_item omi on omi.order_id = om.order_id
        join material_stock ms on ms.material_id = omi.material_id
        join material m on m.id_material = ms.material_id
        join team t on t.id_team = om.team_id
        where ms.deposit_id = :deposit_id and omi.status = :status and ms.deposit_id = om.deposit_id
        
        order by createdAt nulls last, materialIdReservation nulls last;
    """)
    fun getOrdersByStatus(depositId: Long, status: String): List<OrdersByStatusView>

    @Query(
        """
            SELECT 
                material_id_reservation as materialIdReservation,
                cast(null as uuid) as orderId, 
                cast(null as bigint) as materialId, 
                central_material_stock_id as centralMaterialStockId,
                reserved_quantity as requestQuantity,
                direct_execution_id as directExecutionId,
                pre_measurement_id as preMeasurementId,
                status,
                truck_material_stock_id as truckMaterialStockId
            FROM material_reservation
            WHERE material_id_reservation in (:reservationIds)
            
            UNION ALL
            
            SELECT 
                cast(null as bigint) as materialIdReservation, 
                om.order_id as orderId,
                omi.material_id as materialId,
                ms.material_id_stock as centralMaterialStockId, 
                omi.quantity_released as requestQuantity, 
                cast(null as bigint) as directExecutionId, 
                cast(null as bigint) as preMeasurementId, 
                omi.status, 
                tms.material_id_stock as truckMaterialStockId
            FROM order_material om
            JOIN order_material_item omi on omi.order_id = om.order_id
            JOIN material_stock ms 
                ON ms.material_id = omi.material_id and ms.deposit_id = om.deposit_id
            JOIN team t on t.id_team = om.team_id
            JOIN material_stock tms -- truck_material_stock
                ON ms.material_id = omi.material_id and ms.deposit_id = t.deposit_id_deposit
            WHERE EXISTS (
              SELECT 1
              FROM jsonb_to_recordset(:ordersJson::jsonb)
                   AS x(order_id uuid, material_id bigint)
              WHERE x.order_id = omi.order_id
                AND x.material_id = omi.material_id
            )
        """
    )
    fun getOrdersByKeys(
        reservationIds: List<Long>,
        ordersJson: String,
    ): List<OrdersByKeysView>
}

@Repository
interface OrderMaterialItemRepository : CrudRepository<OrderMaterialItem, UUID> {

}