package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.ReserveMaterialJoin
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.Routes
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class DirectExecutionViewModel(
    private val repository: DirectExecutionRepository?,
    private val contractRepository: ContractRepository?,
    private val savedStateHandle: SavedStateHandle? = null,
    mockContractor: String? = null,
    mockCreationDate: String? = null,
    mockStreets: List<DirectExecutionStreet> = emptyList(),
    mockItems: List<ReserveMaterialJoin> = emptyList(),
    mockStreetItems: List<DirectExecutionStreetItem> = emptyList()

) : ViewModel() {
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError
    var installationId by mutableStateOf(savedStateHandle?.get<Long>("id"))
    var creationDate by mutableStateOf(savedStateHandle?.get<String>("creationDate"))
    var contractId by mutableStateOf(savedStateHandle?.get<String>("contractId")?.toLongOrNull())
    var contractor by mutableStateOf(savedStateHandle?.get<String>("contractor"))
    var instructions by mutableStateOf(savedStateHandle?.get<String>("instructions"))

    var street by mutableStateOf<DirectExecutionStreet?>(null)
    var streets by mutableStateOf(mockStreets)
    var streetItems by mutableStateOf(mockStreetItems)

    var hasPosted by mutableStateOf(false)
    var alertModal by mutableStateOf(false)
    var confirmModal by mutableStateOf(false)
    var showFinishForm by mutableStateOf(false)
    var showSignScreen by mutableStateOf(false)

    var loadingCoordinates by mutableStateOf(false)
    var triedToSubmit by mutableStateOf(false)
    var sameStreet by mutableStateOf(false)

    var isLoading by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)

    var reserves by mutableStateOf(mockItems)
    var stockCount by mutableIntStateOf(0)

    var responsible by mutableStateOf<String?>(null)
    var signPath by mutableStateOf<String?>(null)
    var signDate: String? = null

    var responsibleError by mutableStateOf<String?>(null)
    var hasResponsible by mutableStateOf<Boolean?>(null)

    // -> control viewModel
    init {
        setStreets()
        viewModelScope.launch {
            savedStateHandle
                ?.getStateFlow("route_event", null as String?)
                ?.collect { route ->
                    when (route) {
                        Routes.INSTALLATION_HOLDER -> {
                            installationId = null
                            contractId = null
                            contractor = null
                            creationDate = null
                            instructions = null

                            streets = emptyList()
                            reserves = emptyList()

                            street = null
                            streetItems = emptyList()

                            alertModal = false
                            hasPosted = false
                            confirmModal = false
                            showSignScreen = false
                            showFinishForm = false

                            hasResponsible = null
                            responsible = null
                            signPath = null
                            signDate = null
                            sameStreet = false
                            triedToSubmit = false

                            stockCount = 0
                        }

                        Routes.DIRECT_EXECUTION_HOME_SCREEN -> {
                            alertModal = false
                            hasPosted = false
                            confirmModal = false
                            showSignScreen = false
                            showFinishForm = false

                            street = null
                            streetItems = emptyList()

                            hasResponsible = null
                            responsible = null
                            signPath = null
                            signDate = null
                            sameStreet = false
                            triedToSubmit = false

                            setStreets()
                            loadExecutionData()
                        }

                        Routes.DIRECT_EXECUTION_SCREEN_MATERIALS -> {
                            initializeExecution(installationId!!, contractor!!)
                        }
                    }
                }
        }
    }

    private fun initializeExecution(directExecutionId: Long, description: String) {
        if (street == null) {
            street = DirectExecutionStreet(
                address = "",
                latitude = null,
                longitude = null,
                photoUri = null,
                deviceId = UUID.randomUUID().toString(),
                directExecutionId = directExecutionId,
                description = description,
                lastPower = null,
                finishAt = null,
                currentSupply = null,
            )
        }
    }

    fun initializeExecutionSameStreet(currentAddress: String) {
        alertModal = false
        hasPosted = false
        confirmModal = false
        showSignScreen = false
        showFinishForm = false

        hasResponsible = null
        responsible = null
        signPath = null
        signDate = null
        sameStreet = false
        triedToSubmit = false

        street =
            street?.copy(
                address = currentAddress,
                photoUri = null,
                deviceId = UUID.randomUUID().toString(),
                lastPower = null,
                finishAt = null,
                currentSupply = null,
            )
    }

//    fun checkUpdate(currentVersion: Long, callback: (Long?, String?) -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            _syncError.value = null
//            try {
//                isLoading = true
//                val response = repository.checkUpdate(currentVersion)
//                val result = when (response) {
//                    is RequestResult.Timeout -> null to null
//                    is RequestResult.NoInternet -> null to null
//                    is ServerError -> null to null
//                    is RequestResult.UnknownError -> null to null
//                    is RequestResult.SuccessEmptyBody -> null to null
//                    is RequestResult.Success -> response.data.latestVersionCode to response.data.apkUrl
//                }
//
//                withContext(Dispatchers.Main) {
//                    callback(result.first, result.second)
//                }
//            } catch (e: Exception) {
//                isLoading = false
//            } finally {
//                isLoading = false
//            }
//        }
//    }

    private suspend fun getReservesOnce(directExecutionId: Long): List<ReserveMaterialJoin> {
        return withContext(Dispatchers.IO) {
            if (contractRepository?.checkBalance() == true) {
                contractId?.let {
                    contractRepository.getContractItemBalance(it)
                }
            }
            repository?.getReservesOnce(directExecutionId) ?: emptyList()
        }
    }

    fun saveAndPost(coordinates: CoordinatesService?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val (lat, long) = coordinates?.execute() ?: Pair(null, null)
                if (lat != null && long != null) {
                    street = street?.copy(
                        latitude = lat,
                        longitude = long,
                    )
                }
                withContext(Dispatchers.IO) {
                    repository?.saveAndQueueStreet(
                        street?.copy(finishAt = Instant.now().toString()),
                        streetItems
                    )
                }
                showFinishForm = false
                hasPosted = true
            } catch (e: IllegalStateException) {
                isLoading = false
                errorMessage = e.message
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun submitInstallation() {
        isLoading = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository?.queueSubmitInstallation(
                        installationId,
                        responsible,
                        signPath,
                        signDate
                    )
                }
                installationId = null
                street = null
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun loadExecutionData() {
        val id = installationId ?: run {
            errorMessage = "Instalação inválida"
            return
        }

        viewModelScope.launch {
            try {
                isLoading = true

                withContext(Dispatchers.IO) {
                    repository?.setStatus(id, "IN_PROGRESS")
                    getReservesOnce(id)
                }.also { reservesIO ->
                    reserves = reservesIO
                }

            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro inesperado"
            } finally {
                isLoading = false
            }
        }
    }


    fun setStreets() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                withContext(Dispatchers.IO) {
                    repository?.getStreets(installationId) ?: emptyList()
                }.also {
                    streets = it
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro ao carregar as ruas da pré-medição"
            } finally {
                isLoading = false
            }
        }
    }

}
