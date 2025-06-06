package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.api.RequestResult
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

    fun syncContracts(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val response = repository.syncContracts(context)
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

    fun syncContractItems(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val response = repository.syncContractItems(context)
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


    fun loadItemsFromContract(itemsIds: List<String>) {
        viewModelScope.launch {
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
                repository.setStatus(contractId, ExecutionStatus.IN_PROGRESS)
                repository.startAt(contractId, Utils.dateTime.toString(), deviceId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }

    }


}
