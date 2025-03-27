package com.lumos.ui.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.ContractRepository
import com.lumos.domain.model.Contract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun syncContracts() {
        viewModelScope.launch {
            try {
                repository.syncContracts()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    suspend fun getContract(contractId: Long): Contract? {
        return withContext(Dispatchers.IO) {
            try {
                repository.getContract(contractId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                null  // Retorna null em caso de erro
            }
        }
    }


    fun setStatus(contractId: Long, status: String) {
        viewModelScope.launch {
            try {
                repository.setStatus(contractId, status)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun setDate(contractId: Long, updated: String) {
        viewModelScope.launch {
            try {
                repository.setDate(contractId, updated)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun downloadContract(contractId: Long) {
        return
    }


}
