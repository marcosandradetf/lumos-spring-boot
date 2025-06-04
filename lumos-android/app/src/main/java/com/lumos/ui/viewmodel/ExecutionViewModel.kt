package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.api.RequestResult
import com.lumos.data.repository.ExecutionRepository
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Reserve
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

    private val _isLoading = MutableStateFlow<Boolean>(true)
    val isLoading:  StateFlow<Boolean> = _isLoading

    private val _reserves = MutableStateFlow<List<Reserve>>(emptyList())
    val reserves: StateFlow<List<Reserve>> = _reserves

    fun  syncExecutions(context: Context) {
        viewModelScope.launch {
            try {
                when(repository.syncExecutions(context)) {
                    RequestResult.Timeout ->
                }
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    fun loadFlowExecutions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getFlowExecutions().collectLatest { fetched ->
                    _executions.value = fetched // atualiza o estado com os dados obtidos
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun loadFlowReserves(streetId: Long, status: List<String>) {
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

    fun setReserveStatus(streetId: Long, status: String) {
        viewModelScope.launch {
            try {
                repository.setReserveStatus(streetId, status)
            } catch (e: Exception) {
                Log.e("Error setReserveStatus", e.message.toString())
            }
        }
    }

    fun setExecutionStatus(streetId: Long, status: String) {
        viewModelScope.launch {
            try {
                repository.setExecutionStatus(streetId, status)
            } catch (e: Exception) {
                Log.e("Error setExecutionStatus", e.message.toString())
            }
        }
    }

    fun queueSyncFetchReservationStatus(streetId: Long, status: String, context: Context) {
        viewModelScope.launch {
            try {
                repository.queueSyncFetchReservationStatus(context, streetId, status)
            } catch (e: Exception) {
                Log.e("Error queueSyncFetchReservationStatus", e.message.toString())
            }
        }
    }


    suspend fun getExecution(streetId: Long): Execution? {
        return withContext(Dispatchers.IO) {
            try {
                repository.getExecution(streetId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                null  // Retorna null em caso de erro
            }
        }
    }

    fun queueSyncStartExecution(streetId: Long, context: Context) {
        viewModelScope.launch {
            try {
                repository.queueSyncStartExecution(context, streetId)
            } catch (e: Exception) {
                Log.e("Error queueSyncFetchReservationStatus", e.message.toString())
            }
        }
    }

    fun setPhotoUri(photoUri: String, streetId: Long) {
        viewModelScope.launch {
            try {
                repository.setPhotoUri(photoUri, streetId)
            } catch (e: Exception) {
                Log.e("Error setPhotoUri", e.message.toString())
            }
        }
    }

    fun finishMaterial(reserveId: Long, quantityExecuted: Double) {
        viewModelScope.launch {
            try {
                repository.finishMaterial(
                    reserveId = reserveId,
                    quantityExecuted = quantityExecuted
                )
            } catch (e: Exception) {
                Log.e("Error finishMaterial", e.message.toString())
            }
        }
    }

    fun queuePostExecution(streetId: Long, context: Context) {
        viewModelScope.launch {
            try {
                repository.queuePostExecution(context, streetId)
            } catch (e: Exception) {
                Log.e("Error queueSyncFetchReservationStatus", e.message.toString())
            }
        }
    }


}
