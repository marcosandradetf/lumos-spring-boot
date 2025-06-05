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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExecutionViewModel(
    private val repository: ExecutionRepository,

    ) : ViewModel() {
    val executions: StateFlow<List<Execution>> = repository.getFlowExecutions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reserves = MutableStateFlow<List<Reserve>>(emptyList())
    val reserves: StateFlow<List<Reserve>> = _reserves

    private val _isLoadingReserves = MutableStateFlow(false)
    val isLoadingReserves: StateFlow<Boolean> = _isLoadingReserves

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun syncExecutions(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val response = repository.syncExecutions(context)
                when (response) {
                    is RequestResult.Timeout -> _syncError.value =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Tente novamente."

                    is RequestResult.NoInternet -> _syncError.value =
                        "Sem internet no momento. Os dados salvos continuam disponíveis e novos serão buscados automaticamente quando a conexão voltar."

                    is RequestResult.ServerError -> _syncError.value = response.message
                    is RequestResult.Success -> null
                    is RequestResult.UnknownError -> null
                }
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Erro inesperado."
            } finally {
                _isSyncing.value = false
            }
        }
    }


    fun loadFlowReserves(streetId: Long, status: List<String>) {
        viewModelScope.launch {
            _isLoadingReserves.value = true
            try {
                repository.getFlowReserves(streetId, status)
                    .onEach {
                        _isLoadingReserves.value = false
                    } // assim que o primeiro dado chega, desliga o loading
                    .collectLatest { fetched ->
                        _reserves.value = fetched
                    }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                _isLoadingReserves.value = false
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
