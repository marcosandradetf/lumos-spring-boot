package com.lumos.lumosspring.maintenance.repository

import com.lumos.lumosspring.maintenance.model.Maintenance
import com.lumos.lumosspring.maintenance.model.MaintenanceExecutor
import com.lumos.lumosspring.maintenance.model.MaintenanceStreet
import com.lumos.lumosspring.maintenance.model.MaintenanceStreetItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
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

@Repository
interface MaintenanceExecutorRepository : CrudRepository<MaintenanceExecutor, UUID> {
}
