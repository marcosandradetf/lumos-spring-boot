package com.lumos.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.StockRepository
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Material
import com.lumos.service.DepositService
import kotlinx.coroutines.launch

class StockViewModel(
    private val repository: StockRepository,
    private val depositService: DepositService

) : ViewModel() {
    private val _deposits = mutableStateOf<List<Deposit>>(emptyList()) // estado da lista
    val deposits: State<List<Deposit>> = _deposits // estado acessível externamente

    private val _materials = mutableStateOf<List<Material>>(emptyList()) // estado da lista
    val materials: State<List<Material>> = _materials

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

    fun loadMaterials(depositId: Long) {
        viewModelScope.launch {
            try {
                val fetched = repository.getMaterials(depositId)
                _materials.value = fetched // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
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

    fun syncMaterials() {
        viewModelScope.launch {
            try {
                repository.syncMaterials()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    fun firstSync() {

        viewModelScope.launch {
            try {
                repository.firstSync()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

}
