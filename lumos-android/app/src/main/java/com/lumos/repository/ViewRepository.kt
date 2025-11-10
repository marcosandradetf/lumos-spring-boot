package com.lumos.repository

import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.InstallationView
import kotlinx.coroutines.flow.Flow

class ViewRepository(
    private val db: AppDatabase
) {

    fun getFlowInstallations(status: List<String>): Flow<List<InstallationView>> =
        db.viewDao().getInstallationsHolderByStatus(status)

}