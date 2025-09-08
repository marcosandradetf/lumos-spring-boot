package com.lumos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.database.AppDatabase
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.navigation.Routes
import com.lumos.utils.NavEvents
import com.lumos.utils.SyncLoading
import com.lumos.worker.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel(
    private val db: AppDatabase
) : ViewModel() {
    private val _syncItemsTypes = MutableStateFlow<List<String>>(emptyList())
    val syncItemsTypes = _syncItemsTypes

    private val _loading = MutableStateFlow(false)
    val loading = _loading

    private val _message = MutableStateFlow("")
    val message = _message

    private val _syncItems = MutableStateFlow<List<SyncQueueEntity>>(emptyList())
    val syncItems = _syncItems

    init {
        viewModelScope.launch {
            SyncLoading.loading.collect { loading ->
                _loading.value = loading
            }
        }
    }

    fun setMessage(message: String) {
        _message.value = message
        startClearTimer()
    }

    private fun startClearTimer() {
        // Lança uma coroutine no escopo do ViewModel
        viewModelScope.launch {
            // 3. Aguarda 2 segundos (5000 milissegundos)
            kotlinx.coroutines.delay(2000L)

            // 4. Após o tempo, limpa a variável
            _message.value = ""
        }
    }

    fun syncFlowItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.queueDao().getFlowItemsToProcess().collectLatest {
                    _syncItemsTypes.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Problema ao carregar os itens"
                startClearTimer()
            }
        }
    }

    suspend fun getItems(types: List<String>) {
        return withContext(Dispatchers.IO) {
            try {
                db.queueDao().getItem(types).collectLatest {
                    _syncItems.value = it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Problema ao carregar os itens"
                startClearTimer()
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
                    _message.value = "Tarefa reagendada com sucesso."
                    startClearTimer()
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao agendar envio"
                startClearTimer()
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

                    _message.value = "Envio cancelado com sucesso."
                    startClearTimer()
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao cancelar envio"
            }
        }
    }

    fun retryById(id: Long, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.queueDao().existsById(id)) {
                    db.queueDao().retryById(id)
                    SyncManager.enqueueSync(context, true)
                    _message.value = "Tarefa reagendada com sucesso."
                    startClearTimer()
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao agendar envio"
            }
        }
    }

    fun cancelById(id: Long, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.queueDao().existsById(id)) {
                    db.queueDao().deleteById(id)
                    SyncManager.enqueueSync(context, true)
                    _message.value = "Envio cancelado com sucesso."
                    startClearTimer()
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro ao cancelar envio"
            }
        }
    }


}