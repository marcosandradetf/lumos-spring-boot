package com.lumos.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.NotificationRepository
import com.lumos.service.NotificationItem
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository,

    ) : ViewModel() {
    private val _notifications = mutableStateOf<List<NotificationItem>>(emptyList()) // estado da lista
    val notifications: State<List<NotificationItem>> = _notifications // estado acessível externamente

    fun getCountNotifications(): String {
        return notifications.value.size.toString()
    }


    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val fetched = repository.getAll()
                _notifications.value = fetched // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                loadNotifications()
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }


    fun deleteAll() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
                loadNotifications()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }
    }

    fun insert(notificationItem: NotificationItem) : Int {
        var notifications = 0
        viewModelScope.launch {
            try {
                notifications = repository.insert(notificationItem)
                loadNotifications()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }

        return notifications
    }

    fun countNotifications(): Int {
        var notifications = 0
        viewModelScope.launch {
            try {
                notifications = repository.countNotifications()
            } catch (e: Exception) {
                // Tratar erros aqui
            }
        }

        return notifications
    }


}
