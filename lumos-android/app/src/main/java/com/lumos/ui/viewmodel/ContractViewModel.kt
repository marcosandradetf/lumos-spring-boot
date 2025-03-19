package com.lumos.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.StockRepository
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Material
import com.lumos.service.DepositService
import kotlinx.coroutines.launch

class ContractViewModel(
    private val repository: ContractRepository,

) : ViewModel() {
    private val _contracts = mutableStateOf<List<Contract>>(emptyList()) // estado da lista
    val contracts: State<List<Contract>> = _contracts // estado acessível externamente


    fun loadContracts() {
        viewModelScope.launch {
            try {
                val fetched = repository.getContracts()
                _contracts.value = fetched // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun markAsMeasured(contractId: Long) {
        viewModelScope.launch {
            try {
                repository.markAsMeasured(contractId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }


    fun syncMaterials() {
        viewModelScope.launch {
            try {
                repository.syncContracts()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }


}
