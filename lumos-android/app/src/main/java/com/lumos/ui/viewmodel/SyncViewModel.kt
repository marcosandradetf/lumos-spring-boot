package com.lumos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.SyncQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncViewModel(
    private val db: AppDatabase
) : ViewModel() {
    private val _syncItems = MutableStateFlow<List<SyncQueueEntity>>(emptyList())
    val syncItems = _syncItems

    private val _loading = MutableStateFlow(false)
    val loading = _loading

    private val _error = MutableStateFlow("")
    val error = _error


    fun syncFlowItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            try {
                db.queueDao().getFlowItemsToProcess().collectLatest {
                    _syncItems.value = it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Problema ao carregar os itens"
            } finally {
                _loading.value = false
            }
        }
    }

}