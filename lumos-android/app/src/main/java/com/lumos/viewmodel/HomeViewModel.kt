package com.lumos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.domain.model.Contract
import com.lumos.domain.model.InstallationView
import com.lumos.navigation.Routes
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.PreMeasurementInstallationRepository
import com.lumos.repository.ViewRepository
import com.lumos.utils.NavEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ContractRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val preMeasurementInstallationRepository: PreMeasurementInstallationRepository,
    private val viewRepository: ViewRepository

) : ViewModel() {
    private val _contracts = MutableStateFlow<List<Contract>>(emptyList()) // estado da lista
    val contracts: StateFlow<List<Contract>> = _contracts // estado acessível externamente

    private val _installations = MutableStateFlow<List<InstallationView>>(emptyList())
    val installations: StateFlow<List<InstallationView>> = _installations

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _hasInternet = MutableStateFlow(true)
    val hasInternet: StateFlow<Boolean> = _hasInternet

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private var contractsJob: Job? = null
    private var installationsJob: Job? = null

    init {
        loadFlowContracts()
        loadFlowInstallations()

        viewModelScope.launch {
            NavEvents.route.collect { route ->
                if (route != Routes.HOME) {
                    clearItems()
                }
            }
        }
    }

    fun syncContracts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncError.value = null
            try {
                when (repository.syncContracts()) {
                    is RequestResult.Timeout -> null
                    is RequestResult.NoInternet -> null
                    is ServerError -> null
                    is RequestResult.SuccessEmptyBody -> null
                    is RequestResult.Success -> null
                    is RequestResult.UnknownError -> null
                }
            } catch (_: Exception) {
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncInstallations() {
        viewModelScope.launch(Dispatchers.IO) {
            if (syncPreMeasurementInstallations()) syncDirectExecutions()
        }
    }

    private suspend fun syncDirectExecutions() {
        _isSyncing.value = true
        _syncError.value = null
        try {
            when (directExecutionRepository.syncDirectExecutions()) {
                is RequestResult.Timeout -> null
                is RequestResult.NoInternet -> null
                is ServerError -> null
                is RequestResult.SuccessEmptyBody -> null
                is RequestResult.Success -> null
                is RequestResult.UnknownError -> null
            }
        } catch (_: Exception) {
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun syncPreMeasurementInstallations(): Boolean {
        _isSyncing.value = true
        _syncError.value = null
        try {
            when (preMeasurementInstallationRepository.syncExecutions()) {
                is RequestResult.Timeout -> null
                is RequestResult.NoInternet -> null
                is ServerError -> null
                is RequestResult.SuccessEmptyBody -> null
                is RequestResult.Success -> return true
                is RequestResult.UnknownError -> null
            }
        } catch (_: Exception) {
        } finally {
            _isSyncing.value = false
        }

        return false
    }


    fun loadFlowContracts() {
        if (_contracts.value.isNotEmpty()) return

        installationsJob = viewModelScope.launch {
            try {
                repository.getFlowContracts("ACTIVE")
                    .flowOn(Dispatchers.IO)
                    .collectLatest { fetched ->
                        _contracts.value = fetched
                    }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun loadFlowInstallations() {
        if (_installations.value.isNotEmpty()) return

        installationsJob = viewModelScope.launch {
            try {
                viewRepository.getFlowInstallations(listOf("PENDING"))
                    .flowOn(Dispatchers.IO)
                    .collectLatest { fetched ->
                        _installations.value = fetched
                    }
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun clearItems() {
        contractsJob?.cancel() // cancela a coleta
        contractsJob = null
        installationsJob?.cancel() // cancela a coleta
        installationsJob = null

        _contracts.value = emptyList()
        _installations.value = emptyList()
    }

}
