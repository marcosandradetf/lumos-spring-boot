package com.lumos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.repository.StockRepository
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.Stockist
import com.lumos.utils.Utils.checkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class StockViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private var loadJob: Job? = null
    private val _stock = MutableStateFlow<List<MaterialStock>>(emptyList())
    val stock = _stock

    private val _count = MutableStateFlow(0)
    val count = _count

    private val _deposits = MutableStateFlow<List<Deposit>>(emptyList())
    val deposits = _deposits

    private val _stockists = MutableStateFlow<List<Stockist>>(emptyList())
    val stockists = _stockists

    private val _loading = MutableStateFlow(true)
    val loading = _loading

    private val _message = MutableStateFlow("")
    val message = _message

    private val _orderCode = MutableStateFlow("")
    val orderCode = _orderCode

    init {
        loadMaterialsTruckStockControlFlow()
        loadDepositsFlow()
        loadStockistsFlow()
    }

    private fun callSyncStock() {
        viewModelScope.launch(Dispatchers.IO) {
            loading.value = true
            try {
                val response = checkResponse(repository.callGetStock())
                if (response != null) _message.value = response

            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Erro ao tentar sincronizar estoque"
            } finally {
                loading.value = false
            }
        }
    }

    fun loadMaterialsTruckStockControlFlow() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                repository.getMaterialsTruckStockControlFlow()
                    .collectLatest {
                        _stock.value = it
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao buscar materiais"
            }
        }
    }

    fun loadMaterialsOrder() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            var firstEmission = true

            repository.getMaterialsOrder()
                .onStart {
                    _loading.value = true
                }
                .catch { e ->
                    if (e !is CancellationException) {
                        _loading.value = false
                        _message.value = e.message ?: "Erro ao buscar materiais"
                    }
                }
                .collectLatest { list ->
                    _stock.value = list

                    if (firstEmission) {
                        _loading.value = false
                        firstEmission = false
                    }
                }
        }
    }

    fun loadDepositsFlow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getDepositsFlow().collectLatest {
                    _deposits.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Erro ao buscar materiais"
            }
        }
    }

    fun loadStockistsFlow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getStockistsFlow().collectLatest {
                    _stockists.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Erro ao buscar materiais"
            }
        }
    }

    fun hasTypesInQueue(types: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loading.value = repository.hasTypesInQueue(types)
                if (!_loading.value) {
                    callSyncStock()
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Problema ao verificar fila"
            }
        }
    }

    fun saveOrder(materials: List<Long>, depositId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loading.value = true
                _orderCode.value = repository.saveOrder(materials, depositId)
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro inesperado"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clear() {
        _orderCode.value = ""
        _message.value = ""
    }


}