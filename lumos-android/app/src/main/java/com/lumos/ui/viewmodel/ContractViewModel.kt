package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.Status
import com.lumos.domain.model.Contract
import com.lumos.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContractViewModel(
    private val repository: ContractRepository,

    ) : ViewModel() {
    private val _contracts = MutableStateFlow<List<Contract>>(emptyList()) // estado da lista
    val contracts: StateFlow<List<Contract>> = _contracts // estado acessível externamente

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError


    fun loadFlowContracts(status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false
            try {
                repository.getFlowContracts(status).collectLatest { fetched ->
                    _contracts.value = fetched
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                _hasError.value = true
                _isLoading.value = false
            }
        }
    }

    fun queueSyncContracts(context: Context) {
        viewModelScope.launch {
            try {
                repository.queueSyncContracts(context)
            } catch (e: Exception) {
                Log.e("Erro view model - queueSyncContracts", e.message.toString())
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

    fun downloadContract(contractId: Long) {
        return
    }

    fun startPreMeasurement(contractId: Long, deviceId: String) {
        viewModelScope.launch {
            try {
                repository.setStatus(contractId, Status.IN_PROGRESS)
                repository.startAt(contractId, Utils.dateTime.toString(), deviceId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }

    }




}
