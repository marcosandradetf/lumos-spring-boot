package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.stock.entities.OrderMaterial
import com.lumos.lumosspring.stock.entities.OrderMaterialItem
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface OrderMaterialRepository : CrudRepository<OrderMaterial, UUID> {
}

interface OrderMaterialItemRepository : CrudRepository<OrderMaterialItem, UUID> {
}