package com.lumos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.worker.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel(
    private val db: AppDatabase
) : ViewModel() {
    private val _syncItems = MutableStateFlow<List<String>>(emptyList())
    val syncItems = _syncItems

    private val _loading = MutableStateFlow(false)
    val loading = _loading

    private val _message = MutableStateFlow("")
    val message = _message

    fun syncFlowItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.queueDao().getFlowItemsToProcess().collectLatest {
                    _syncItems.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Problema ao carregar os itens"
            }
        }
    }

    suspend fun getItems(type: String): List<SyncQueueEntity> {
        return withContext(Dispatchers.IO) {
            _loading.value = true
            try {
                db.queueDao().getItem(type)
            } catch (e: Exception) {
                _loading.value = false
                _message.value = e.message ?: "Problema ao carregar os itens"
                emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun getStreets(streetIds: List<Long>): List<DirectExecutionStreet> {
        return withContext(Dispatchers.IO) {
            _loading.value = true
            try {
                db.directExecutionDao().getStreets(streetIds)
            } catch (e: Exception) {
                _loading.value = false
                _message.value = e.message ?: "Problema ao carregar as ruas"
                emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun retry(relatedId: Long, type: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.queueDao().retry(relatedId, type)
                SyncManager.enqueueSync(context, true)
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao agendar envio"
            } finally {
                _message.value = "Tarefa reagendada com sucesso."
                _message.value = ""
            }
        }
    }


}