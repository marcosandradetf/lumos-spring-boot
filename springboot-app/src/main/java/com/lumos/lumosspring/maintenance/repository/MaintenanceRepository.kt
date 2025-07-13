package com.lumos.lumosspring.maintenance.repository

import com.lumos.lumosspring.maintenance.entities.Maintenance
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreet
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreetItem
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MaintenanceRepository : CrudRepository<Maintenance, UUID> {
}

interface MaintenanceStreetRepository : CrudRepository<MaintenanceStreet, UUID> {
}

interface MaintenanceStreetItemRepository : CrudRepository<MaintenanceStreetItem, UUID> {
}