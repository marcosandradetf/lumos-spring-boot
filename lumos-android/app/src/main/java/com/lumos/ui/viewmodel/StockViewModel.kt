package com.lumos.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.StockRepository
import com.lumos.domain.model.Deposit
import com.lumos.service.DepositService
import kotlinx.coroutines.launch

class StockViewModel(
    private val repository: StockRepository,
    private val depositService: DepositService

) : ViewModel() {
    private val _deposits = mutableStateOf<List<Deposit>>(emptyList()) // estado da lista
    val deposits: State<List<Deposit>> = _deposits // estado acessível externamente

    // Função para carregar os depósitos
    fun loadDeposits() {
        viewModelScope.launch {
            try {
                val fetchedDeposits = depositService.getDeposits()
                _deposits.value = fetchedDeposits // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }


    fun syncDeposits() {
        viewModelScope.launch {
            try {
                depositService.syncDeposits()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

}
