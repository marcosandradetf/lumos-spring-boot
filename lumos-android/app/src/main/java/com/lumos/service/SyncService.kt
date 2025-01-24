package com.lumos.service

import android.content.Context
import com.lumos.data.repository.MeasurementRepository
import com.lumos.domain.usecases.SyncDataUseCase


class SyncService(
    private val context: Context,
    private val repository: MeasurementRepository
) {
    suspend fun sync() {
        SyncDataUseCase(context, repository).execute()
    }
}