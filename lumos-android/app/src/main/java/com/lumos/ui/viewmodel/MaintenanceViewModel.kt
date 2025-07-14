package com.lumos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.MaintenanceRepository
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class MaintenanceViewModel(
    private val repository: MaintenanceRepository
) : ViewModel() {
    private val _maintenanceId = MutableStateFlow<UUID?>(null)
    val maintenanceId: StateFlow<UUID?> = _maintenanceId

    private val _loading = MutableStateFlow(true)
    val loading = _loading

    private val _streetCreated = MutableStateFlow(false)
    val streetCreated = _streetCreated

    private val _contractSelected = MutableStateFlow(false)
    val contractSelected = _contractSelected

    private val _finish = MutableStateFlow(false)
    val finish = _finish

    private val _message = MutableStateFlow<String?>(null)
    val message = _message

    private val _maintenances = MutableStateFlow<List<Maintenance>>(emptyList())
    val maintenances = _maintenances

    private val _maintenanceStreets = MutableStateFlow<List<MaintenanceStreet>>(emptyList())
    val maintenanceStreets: StateFlow<List<MaintenanceStreet>> = _maintenanceStreets

    fun loadMaintenances(status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            repository.getFlowMaintenance(status)
                .catch { e ->
                    _loading.value = false
                    _message.value = e.message ?: "Erro ao carregar manutenções"
                    _maintenances.value = emptyList()
                }
                .collectLatest {
                    _maintenances.value = it
                    _loading.value = false
                }
        }
    }

    fun loadMaintenanceStreets(maintenanceId: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.getFlowStreets(maintenanceId)
                .catch { e ->
                    _loading.value = false
                    _message.value = e.message ?: "Erro ao carregar ruas"
                    _maintenanceStreets.value = emptyList()
                }
                .collectLatest {
                    _maintenanceStreets.value = it
                    _loading.value = false
                }
        }
    }

    fun insertMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            try {
                _maintenanceId.value = UUID.fromString(maintenance.maintenanceId)
                _loading.value = true
                repository.insertMaintenance(maintenance)
                _contractSelected.value = true
            } catch (e: Exception) {
                _message.value = e.message ?: ""
            } finally {
                _loading.value = false
            }
        }
    }

    fun insertMaintenanceStreet(street: MaintenanceStreet, items: List<MaintenanceStreetItem>) {
        viewModelScope.launch {
            try {
                _loading.value = true
                repository.insertMaintenanceStreet(street, items)
                _streetCreated.value = true
            } catch (e: Exception) {
                _message.value = e.message ?: "Erro inesperado"
            } finally {
                _loading.value = false
            }
        }
    }

    fun queuePostMaintenance(maintenanceId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                repository.queuePostMaintenance(maintenanceId)
                _finish.value = true
            } catch (e: Exception) {
                _message.value = e.message ?: ""
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearViewModel() {
        _message.value = ""
        _finish.value = false
        _streetCreated.value = false
    }

    fun setMaintenanceId(uuid: UUID) {
        _maintenanceId.value = uuid
    }


}