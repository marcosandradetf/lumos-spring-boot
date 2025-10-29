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


    @Query(
        """
            -- Installation requests
            select 
                'INSTALAÇÃO - ' || upper(coalesce(cd.contractor, pm.city)) as contractor,
                mr.material_id_reservation,
                cast(null as uuid) as order_id,
                mr.reserved_quantity as request_quantity,
                mr.description,
                m.id_material as material_id,
                m.material_name || ' ' || coalesce(coalesce(m.material_power, m.material_length), '') as material_name,
                t.team_name,
                ms.stock_quantity,
                mr.status,
                cast(null as timestamp) as created_at
            from material_reservation mr
            join material_stock ms on ms.material_id_stock = mr.central_material_stock_id
            join material m on m.id_material = ms.material_id
            left join direct_execution de on mr.direct_execution_id = de.direct_execution_id
            left join pre_measurement pm on pm.pre_measurement_id = mr.pre_measurement_id
            join team t on t.id_team = mr.team_id
            left join contract cd on de.contract_id = cd.contract_id
            where ms.deposit_id = :depositId and mr.status = :status
        
            UNION ALL
        
            -- Team requests
            select 
                cast(null as text) as contractor,
                cast(null as bigint) as material_id_reservation,
                om.order_id,
                omi.quantity_released as request_quantity,
                om.order_code as description,
                m.id_material as material_id,
                m.material_name || ' ' || coalesce(coalesce(m.material_power, m.material_length), '') as material_name,
                t.team_name as team_name,
                ms.stock_quantity as stock_quantity,
                omi.status,
                om.created_at
            from order_material om
            join order_material_item omi on omi.order_id = om.order_id
            join material_stock ms on ms.material_id = omi.material_id
            join material m on m.id_material = ms.material_id
            join team t on t.id_team = om.team_id
            where ms.deposit_id = :depositId and omi.status = :status and ms.deposit_id = om.deposit_id
        
            order by created_at nulls last, material_id_reservation nulls last;
        """
    )
    fun getOrdersByStatus(depositId: Long, status: String): List<OrdersByStatusView>


    @Query(
        """
            SELECT 
                mr.material_id_reservation,
                cast(null as uuid) as order_id, 
                cast(null as bigint) as "material_id", 
                mr.central_material_stock_id,
                mr.reserved_quantity as request_quantity,
                mr.direct_execution_id,
                mr.pre_measurement_id,
                mr.status,
                mr.truck_material_stock_id,
                m.material_name || ' ' || coalesce(coalesce(m.material_power, m.material_length), '') as material_name
            FROM material_reservation mr
            JOIN material_stock ms ON mr.central_material_stock_id = ms.material_id_stock
            JOIN material m ON ms.material_id = m.id_material
            WHERE mr.material_id_reservation in (:reservationIds)
            
            UNION ALL
            
            SELECT 
                cast(null as bigint) as material_id_reservation, 
                om.order_id,
                omi.material_id,
                ms.material_id_stock as central_material_stock_id, 
                omi.quantity_released as request_quantity, 
                cast(null as bigint) as direct_execution_id, 
                cast(null as bigint) as pre_measurement_id, 
                omi.status, 
                tms.material_id_stock as truck_material_stock_id,
                m.material_name || ' ' || coalesce(coalesce(m.material_power, m.material_length), '') as material_name
            FROM order_material om
            JOIN order_material_item omi on omi.order_id = om.order_id
            JOIN material m on m.id_material = omi.material_id
            JOIN team t on t.id_team = om.team_id
            JOIN material_stock ms 
                ON ms.material_id = omi.material_id and ms.deposit_id = om.deposit_id
            JOIN material_stock tms -- truck_material_stock
                ON tms.material_id = omi.material_id and tms.deposit_id = t.deposit_id_deposit
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