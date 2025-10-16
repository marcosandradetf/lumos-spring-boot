package com.lumos.repository

import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.InstallationView
import kotlinx.coroutines.flow.Flow

class ViewRepository(
    private val db: AppDatabase
) {

    fun getFlowInstallations(): Flow<List<InstallationView>> =
        db.viewDao().getInstallationsHolder()

}