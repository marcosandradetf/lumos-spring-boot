package com.lumos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.StockRepository
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.Stockist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StockViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private val _stock = MutableStateFlow<List<MaterialStock>>(emptyList())
    val stock = _stock

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

    fun callSyncStock() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.queueGetStock()
            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Erro ao tentar sincronizar estoque"
            }
        }
    }

    fun loadStockFlow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getMaterialsFlow().collectLatest {
                    _stock.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Erro ao buscar materiais"
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

    fun getFlowExistsTypeInQueue(types: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getFlowExistsTypeInQueue(types).collectLatest {
                    _loading.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "ViewModel - Problema ao verificar fila"
            }
        }
    }

    fun saveOrder(materials: List<Long>, depositId:Long) {
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