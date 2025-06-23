package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.api.RequestResult
import com.lumos.data.api.RequestResult.ServerError
import com.lumos.data.repository.DirectExecutionRepository
import com.lumos.data.repository.ReservationStatus
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.ExecutionHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectExecutionViewModel(
    private val repository: DirectExecutionRepository,

    ) : ViewModel() {
    val directExecutions: StateFlow<List<ExecutionHolder>> = repository.getFlowDirectExecutions()
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
                val response = repository.syncDirectExecutions()
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


//    fun setExecutionStatus(streetId: Long, status: String) {
//        viewModelScope.launch {
//            try {
//                repository.setExecutionStatus(streetId, status)
//            } catch (e: Exception) {
//                Log.e("Error setExecutionStatus", e.message.toString())
//            }
//        }
//    }


    suspend fun getExecution(contractId: Long): DirectExecution? {
        return withContext(Dispatchers.IO) {
            try {
                repository.getExecution(contractId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                null  // Retorna null em caso de erro
            }
        }
    }

//    fun queueSyncStartExecution(streetId: Long, context: Context) {
//        viewModelScope.launch {
//            try {
//                withContext(Dispatchers.IO) {
//                    repository.queueSyncStartExecution(streetId)
//                }
//            } catch (e: Exception) {
//                Log.e("Error queueSyncFetchReservationStatus", e.message.toString())
//            }
//        }
//    }

    suspend fun getReservesOnce(contractId: Long): List<DirectReserve> {
        return withContext(Dispatchers.IO) {
            repository.getReservesOnce(contractId)
        }
    }

    fun saveAndPost(
        street: DirectExecutionStreet,
        items: List<DirectExecutionStreetItem>,
        onPostExecuted: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoadingReserves.value = true
            try {
                withContext(Dispatchers.IO) {
                    val streetId = repository.createStreet(street)
                    if(streetId <= 0) {
                        throw IllegalStateException("Endereço informado já enviado.")
                    }

                    for (item in items) {
                        repository.debitMaterial(
                            item.materialStockId,
                            item.contractItemId,
                            item.quantityExecuted
                        )
                        repository.createStreetItem(
                            item.copy(
                                directStreetId = streetId
                            )
                        )
                    }

                    repository.queuePostDirectExecution(streetId)
                }
                onPostExecuted()
            } catch (e: IllegalStateException) {
                onError(e.message ?: "Erro inesperado")
            } catch (e: Exception) {
                onError("Erro: ${e.localizedMessage}")
            } finally {
                _isLoadingReserves.value = false
            }
        }
    }


}
