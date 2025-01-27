package com.lumos.service

import android.content.Context
import com.lumos.domain.model.Deposit
import com.lumos.data.repository.StockRepository


class DepositService(
    private val context: Context,
    private val repository: StockRepository
) {

    suspend fun getDepositByRegion(regionName: String): Deposit? {
        return repository.getDepositByRegion(regionName)
    }

    suspend fun getDeposits(): List<Deposit> {
        return repository.getAllDeposits()
    }

    suspend fun syncDeposits() {
        repository.syncDeposits()
    }

}
