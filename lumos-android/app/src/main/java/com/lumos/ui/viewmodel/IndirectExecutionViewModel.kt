package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.api.RequestResult
import com.lumos.data.api.RequestResult.ServerError
import com.lumos.data.repository.IndirectExecutionRepository
import com.lumos.domain.model.ExecutionHolder
import com.lumos.domain.model.IndirectExecution
import com.lumos.domain.model.IndirectReserve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IndirectExecutionViewModel(
    private val repository: IndirectExecutionRepository,

    ) : ViewModel() {
    val executions: StateFlow<List<ExecutionHolder>> = repository.getFlowExecutions()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingReserves = MutableStateFlow(false)
    val isLoadingReserves: StateFlow<Boolean> = _isLoadingReserves

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun syncExecutions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val response = repository.syncExecutions()
                when (response) {
                    is RequestResult.Timeout -> _syncError.value =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Mas você pode continuar com o que tempos aqui - ou puxe para atualizar agora mesmo."

                    is RequestResult.NoInternet -> _syncError.value =
                        "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo."

                    is ServerError -> _syncError.value = response.message
                    is RequestResult.Success -> _syncError.value = null
                    is RequestResult.UnknownError -> _syncError.value = null
                    is RequestResult.SuccessEmptyBody -> {
                        ServerError(204, "Resposta 204 inesperada")
                    }
                }
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Erro inesperado."
            } finally {
                _isSyncing.value = false
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
                repository.queueSyncFetchReservationStatus(streetId, status)
            } catch (e: Exception) {
                Log.e("Error queueSyncFetchReservationStatus", e.message.toString())
            }
        }
    }


    suspend fun getExecution(streetId: Long): IndirectExecution? {
        return withContext(Dispatchers.IO) {
            try {
                repository.getExecution(streetId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                null  // Retorna null em caso de erro
            }
        }
    }

    fun setPhotoUri(photoUri: String, streetId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.setPhotoUri(photoUri, streetId)
                }
            } catch (e: Exception) {
                Log.e("Error setPhotoUri", e.message.toString())
            }
        }
    }

    suspend fun getReservesOnce(streetId: Long): List<IndirectReserve> {
        return withContext(Dispatchers.IO) {
            repository.getReservesOnce(streetId)
        }
    }

    fun finishAndCheckPostExecution(
        reserveId: Long,
        quantityExecuted: Double,
        streetId: Long,
        context: Context,
        hasPosted: Boolean,
        onReservesUpdated: (List<IndirectReserve>) -> Unit,
        onPostExecuted: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoadingReserves.value = true
            try {
                val reserves = withContext(Dispatchers.IO) {
                    repository.finishMaterial(reserveId, quantityExecuted)
                    repository.getReservesOnce(streetId)
                }

                if (!hasPosted && reserves.isEmpty()) {
                    try {
                        withContext(Dispatchers.IO) {
                            repository.queuePostExecution(streetId)
                        }
                        onPostExecuted()
                    } catch (e: Exception) {
                        Log.e("ViewModel", "Erro ao enviar execução", e)
                        onError("Erro ao enviar execução: ${e.localizedMessage}")
                    }
                } else {
                    onReservesUpdated(reserves)
                }

            } catch (e: Exception) {
                Log.e("ViewModel", "Erro ao finalizar material ou buscar dados", e)
                onError("Erro ao finalizar material: ${e.localizedMessage}")
            } finally {
                _isLoadingReserves.value = false
            }
        }
    }

    suspend fun getExecutionsByContract(lng: Long): List<IndirectExecution> {
        return withContext(Dispatchers.IO) {
            repository.getExecutionsByContract(lng)
        }
    }


}
