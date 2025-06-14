package com.lumos.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.api.RequestResult
import com.lumos.data.api.RequestResult.ServerError
import com.lumos.data.repository.ContractRepository
import com.lumos.data.repository.ExecutionStatus
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Item
import com.lumos.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContractViewModel(
    private val repository: ContractRepository,

    ) : ViewModel() {
    private val _contracts = MutableStateFlow<List<Contract>>(emptyList()) // estado da lista
    val contracts: StateFlow<List<Contract>> = _contracts // estado acessível externamente

    private val _items = MutableStateFlow<List<Item>>(emptyList()) // estado da lista
    val items: StateFlow<List<Item>> = _items

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun syncContracts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val response = repository.syncContracts()
                when (response) {
                    is RequestResult.Timeout -> _syncError.value =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Tente novamente."
                    is RequestResult.NoInternet -> _syncError.value =
                        "Sem internet no momento. Os dados salvos continuam disponíveis e novos serão buscados automaticamente quando a conexão voltar."
                    is ServerError -> _syncError.value = response.message
                    is RequestResult.SuccessEmptyBody -> {
                        ServerError(204, "Resposta 204 inesperada")
                    }
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

    fun syncContractItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val response = repository.syncContractItems()
                when (response) {
                    is RequestResult.Timeout -> _syncError.value =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Tente novamente."
                    is RequestResult.NoInternet -> _syncError.value =
                        "Sem internet no momento. Os dados salvos continuam disponíveis e novos serão buscados automaticamente quando a conexão voltar."
                    is RequestResult.ServerError -> _syncError.value = response.message
                    is RequestResult.SuccessEmptyBody -> {
                        ServerError(204, "Resposta 204 inesperada")
                    }
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


    fun loadItemsFromContract(itemsIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getItemsFromContract(itemsIds).collectLatest { entity ->
                    _items.value = entity
                }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun loadFlowContracts(status: String) {
        viewModelScope.launch {
            try {
                repository.getFlowContracts(status)
                    .flowOn(Dispatchers.IO)
                    .collectLatest { fetched ->
                    _contracts.value = fetched
                }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun loadFlowContractsByExecution(executionsIds: List<Long>) {
        viewModelScope.launch {
            try {
                repository.getFlowContractsByExecution(executionsIds)
                    .flowOn(Dispatchers.IO)
                    .collectLatest { fetched ->
                        _contracts.value = fetched
                    }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
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


    fun downloadContract(contractId: Long) {
        return
    }

    fun startPreMeasurement(contractId: Long, deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setStatus(contractId, ExecutionStatus.IN_PROGRESS)
                repository.startAt(contractId, Utils.dateTime.toString(), deviceId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }

    }


}
