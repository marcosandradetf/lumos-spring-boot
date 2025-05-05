package com.lumos.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.ExecutionRepository
import com.lumos.data.repository.Status
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
import com.lumos.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExecutionViewModel(
    private val repository: ExecutionRepository,

    ) : ViewModel() {
    private val _executions = MutableStateFlow<List<Execution>>(emptyList()) // estado da lista
    val executions: StateFlow<List<Execution>> = _executions // estado acessível externamente

    private val _reserves = MutableStateFlow<List<Reserve>>(emptyList())
    val reserves: StateFlow<List<Reserve>> = _reserves

    fun syncExecutions() {
        viewModelScope.launch {
            try {
                repository.syncExecutions()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    fun loadFlowExecutions(status: String) {
        viewModelScope.launch {
            try {
                repository.getFlowExecutions(status).collectLatest { fetched ->
                    _executions.value = fetched // atualiza o estado com os dados obtidos
                }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun loadFlowReserves(streetId: Long, status: String) {
        viewModelScope.launch {
            try {
                repository.getFlowReserves(streetId, status).collectLatest { fetched ->
                    _reserves.value = fetched // atualiza o estado com os dados obtidos
                }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

}
