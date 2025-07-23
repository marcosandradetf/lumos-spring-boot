package com.lumos.lumosspring.maintenance.repository

import com.lumos.lumosspring.maintenance.entities.Maintenance
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreet
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreetItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MaintenanceRepository : CrudRepository<Maintenance, UUID> {
}

@Repository
interface MaintenanceStreetRepository : CrudRepository<MaintenanceStreet, UUID> {
}

@Repository
interface MaintenanceStreetItemRepository : CrudRepository<MaintenanceStreetItem, UUID> {
}