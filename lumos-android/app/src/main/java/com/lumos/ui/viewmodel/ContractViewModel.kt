package com.lumos.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.ContractRepository
import com.lumos.domain.model.Contract
import kotlinx.coroutines.launch

class ContractViewModel(
    private val repository: ContractRepository,

    ) : ViewModel() {
    private val _contracts = mutableStateOf<List<Contract>>(emptyList()) // estado da lista
    val contracts: State<List<Contract>> = _contracts // estado acessível externamente


    fun loadContracts(status: String) {
        viewModelScope.launch {
            try {
                val fetched = repository.getContracts(status)
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


    fun syncContracts() {
        viewModelScope.launch {
            try {
                repository.syncContracts()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    fun getContract(contractId: Long): Contract? {
        return repository.getContract(contractId)
    }


}
