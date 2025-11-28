package com.lumos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceJoin
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import com.lumos.domain.service.CoordinatesService
import com.lumos.repository.MaintenanceRepository
import com.lumos.ui.maintenance.MaintenanceUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class MaintenanceUiState(
    val maintenanceId: UUID? = null,
    val loading: Boolean = false,
    val streetCreated: Boolean = false,
    val contractSelected: Boolean = false,
    val finish: Boolean = false,
    val message: String? = null,
    val maintenances: List<MaintenanceJoin> = emptyList(),
    val maintenanceStreets: List<MaintenanceStreet> = emptyList(),
    val screenState: MaintenanceUIState? = null
)

class MaintenanceViewModel(
    private val repository: MaintenanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaintenanceUiState(loading = true))

    var uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    init {
        loadMaintenances("IN_PROGRESS")
        loadMaintenanceStreets()
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(loading = isLoading) }
    }

    fun setScreenState(state: MaintenanceUIState) = _uiState.update { it.copy(screenState = state) }

    fun setMessage(message: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(message = message) }

            delay(5000)

            _uiState.update { it.copy(message = null) }
        }
    }


    private fun setMaintenances(list: List<MaintenanceJoin>) {
        _uiState.update { it.copy(maintenances = list) }
    }

    private fun setMaintenanceStreets(list: List<MaintenanceStreet>) {
        _uiState.update { it.copy(maintenanceStreets = list) }
    }

    fun setMaintenanceId(id: UUID?) {
        _uiState.update { it.copy(maintenanceId = id) }
    }

    private fun setStreetCreated(value: Boolean) {
        _uiState.update { it.copy(streetCreated = value) }
    }

    fun setContractSelected(value: Boolean) {
        _uiState.update { it.copy(contractSelected = value) }
    }

    private fun setFinish(value: Boolean) {
        _uiState.update { it.copy(finish = value) }
    }

    private fun loadMaintenances(status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            setLoading(true)
            repository.getFlowMaintenance(status)
                .catch { e ->
                    setLoading(false)
                    setMessage(e.message ?: "Erro ao carregar manutenções")
                    setMaintenances(emptyList())
                }
                .collectLatest {
                    setMaintenances(it)
                    setLoading(false)
                }
        }
    }

    private fun loadMaintenanceStreets() {
        viewModelScope.launch {
            setLoading(true)
            repository.getFlowStreets()
                .catch { e ->
                    setLoading(false)
                    setMessage(e.message ?: "Erro ao carregar ruas")
                    setMaintenanceStreets(emptyList())
                }
                .collectLatest {
                    setMaintenanceStreets(it)
                    setLoading(false)
                }
        }
    }

    fun insertMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val uuid = repository.getMaintenanceIdByContractId(maintenance.contractId)
                if (uuid != null) {
                    setMaintenanceId(UUID.fromString(uuid))
                    setContractSelected(true)
                    return@launch
                }
                setMaintenanceId(UUID.fromString(maintenance.maintenanceId))
                repository.insertMaintenance(maintenance)
                setContractSelected(true)
            } catch (e: Exception) {
                if (e.message?.lowercase()?.contains("unique") == true) {
                    setMessage("Manutenção já salva anteriormente")
                } else {
                    setMessage(e.message ?: "")
                }
            } finally {
                setLoading(false)
            }
        }
    }

    fun insertMaintenanceStreet(
        street: MaintenanceStreet,
        items: List<MaintenanceStreetItem>,
        coordinates: CoordinatesService
    ) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val (lat, long) = coordinates.execute()
                withContext(Dispatchers.IO) {
                    repository.insertMaintenanceStreet(
                        street.copy(
                            latitude = lat ?: street.latitude,
                            longitude = long ?: street.longitude
                        ), items
                    )
                }
                setStreetCreated(true)
            } catch (e: Exception) {
                if (e.message?.lowercase()?.contains("unique") == true) {
                    setMessage("Esse ponto já foi salvo - Informe outro ponto ou outro número")
                } else {
                    setMessage(e.message ?: "")
                }
            } finally {
                setLoading(false)
            }
        }
    }

    fun finishMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            try {
                setLoading(true)
                repository.finishMaintenance(maintenance)
                setFinish(true)
            } catch (e: Exception) {
                setMessage(e.message ?: "Erro inesperado")
            } finally {
                setLoading(false)
            }
        }
    }

    fun resetAllState() {
        _uiState.value = MaintenanceUiState()
    }

    fun resetFormState() {
        _uiState.update {
            it.copy(
                maintenanceId = null,
                message = null,
                finish = false,
                streetCreated = false,
                contractSelected = false
            )
        }
    }

}

