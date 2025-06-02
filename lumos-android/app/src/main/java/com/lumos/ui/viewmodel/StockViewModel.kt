package com.lumos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.StockRepository
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Material
import com.lumos.worker.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StockViewModel(
    private val repository: StockRepository,

) : ViewModel() {
    private val _materials = MutableStateFlow<List<Material>>(emptyList()) // estado da lista
    val materials: StateFlow<List<Material>> = _materials

    fun loadMaterialsOfContract(powers: List<String>, lengths: List<String>) {
        viewModelScope.launch {
            try {
                repository.getMaterialsOfContract(powers, lengths).collectLatest { entity ->
                    _materials.value = entity
                }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }


    fun syncMaterials() {
        viewModelScope.launch {
            try {
                repository.syncMaterials()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    fun queueSyncStock(context: Context) {
        viewModelScope.launch {
            try {
                repository.queueSyncStock(context)
            } catch (e: Exception) {
                Log.e("Erro view model - queueSyncStock", e.message.toString())
            }
        }
    }


}
