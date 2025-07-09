package com.lumos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.MaintenanceRepository
import com.lumos.domain.model.MaterialStock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MaintenanceViewModel(
    private val repository: MaintenanceRepository
) : ViewModel() {

    private val _stock = MutableStateFlow<List<MaterialStock>>(emptyList())
    val stock = _stock

    private val _loading = MutableStateFlow(true)
    val loading = _loading

    private val _message = MutableStateFlow("")
    val message = _message

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
}