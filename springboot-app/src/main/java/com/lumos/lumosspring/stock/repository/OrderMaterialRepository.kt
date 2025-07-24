package com.lumos.lumosspring.stock.repository

import com.lumos.lumosspring.stock.entities.OrderMaterial
import com.lumos.lumosspring.stock.entities.OrderMaterialItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderMaterialRepository : CrudRepository<OrderMaterial, UUID> {
}

@Repository
interface OrderMaterialItemRepository : CrudRepository<OrderMaterialItem, UUID> {
}