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

    suspend fun getItems(types: List<String>): List<SyncQueueEntity> {
        return withContext(Dispatchers.IO) {
            _loading.value = true
            try {
                db.queueDao().getItem(types)
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
                if (db.queueDao().exists(relatedId, type)) {
                    db.queueDao().retry(relatedId, type)
                    SyncManager.enqueueSync(context, true)
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao agendar envio"
            } finally {
                _message.value = "Tarefa reagendada com sucesso."
            }
        }
    }

    fun cancel(relatedId: Long, type: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.queueDao().exists(relatedId, type)) {
                    db.queueDao().deleteByRelatedId(relatedId, type)
                    db.directExecutionDao().deleteStreet(relatedId)
                    SyncManager.enqueueSync(context, true)
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao cancelar envio"
            } finally {
                _message.value = "Envio cancelado com sucesso."
            }
        }
    }

    fun retryById(id: Long, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.queueDao().existsById(id)) {
                    db.queueDao().retryById(id)
                    SyncManager.enqueueSync(context, true)
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao agendar envio"
            } finally {
                _message.value = "Tarefa reagendada com sucesso."
            }
        }
    }

    fun cancelById(id: Long,context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.queueDao().existsById(id)) {
                    db.queueDao().deleteById(id)
                    SyncManager.enqueueSync(context, true)
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao cancelar envio"
            } finally {
                _message.value = "Envio cancelado com sucesso."
            }
        }
    }





}