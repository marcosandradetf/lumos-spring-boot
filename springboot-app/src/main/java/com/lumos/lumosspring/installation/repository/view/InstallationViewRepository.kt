package com.lumos.lumosspring.installation.repository.view

import com.lumos.lumosspring.installation.view.InstallationView
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InstallationViewRepository : CrudRepository<InstallationView, Long> {
    fun findInstallationViewByReservationManagementIdIn(reservationManagementIds: MutableCollection<Long>): List<InstallationView>
}