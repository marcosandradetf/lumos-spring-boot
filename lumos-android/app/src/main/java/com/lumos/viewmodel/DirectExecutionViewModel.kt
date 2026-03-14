package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.ReserveMaterialJoin
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.Routes
import com.lumos.repository.ContractRepository
import com.lumos.repository.DirectExecutionRepository
import com.lumos.repository.StockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import kotlin.collections.emptyList

class DirectExecutionViewModel(
    private val repository: DirectExecutionRepository?,
    private val contractRepository: ContractRepository?,
    savedStateHandle: SavedStateHandle?,
    stockRepository: StockRepository?,

    mockContractor: String? = null,
    mockCreationDate: String? = null,
    mockStreets: List<DirectExecutionStreet> = emptyList(),
    mockItems: List<ReserveMaterialJoin> = emptyList(),
    mockStreetItems: List<DirectExecutionStreetItem> = emptyList(),
    mockStockData: List<MaterialStock> = emptyList()

) : ViewModel() {
    private val _installationId = MutableStateFlow(savedStateHandle?.get<Long>("id"))
    val installationId: Long? get() = _installationId.value
    var creationDate by mutableStateOf(savedStateHandle?.get<String>("creationDate"))
    var contractId by mutableStateOf(savedStateHandle?.get<String>("contractId")?.toLongOrNull())
    var contractor by mutableStateOf(savedStateHandle?.get<String>("contractor"))
    var instructions by mutableStateOf(savedStateHandle?.get<String>("instructions"))

    val stock = stockRepository!!.getMaterialsFlow()
    val contracts = contractRepository!!.getFlowContractsForPreMeasurement("ACTIVE")

    var street by mutableStateOf<DirectExecutionStreet?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val streets = _installationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            repository!!.getStreets(id)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
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
    var acceptedResponsibilityTerm by mutableStateOf(false)

    // -> control viewModel
    fun onCreateInstallation() {
        _installationId.value = null
        contractId = null
        contractor = null
        creationDate = null
        instructions = null

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
        acceptedResponsibilityTerm = false
    }

    fun onHomeScreen() {
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

        loadExecutionData()
    }

    fun onExecutionScreen() {
        val id = installationId ?: return
        val contractorName = contractor ?: return

        initializeExecution(id, contractorName)
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
                comment = null
            )
        }
    }

    fun startNewExecution(currentAddress: String?) {
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
        streetItems = emptyList()

        street =
            street?.copy(
                address = currentAddress ?: "",
                photoUri = null,
                deviceId = UUID.randomUUID().toString(),
                lastPower = null,
                finishAt = null,
                currentSupply = null,
                comment = null
            )
    }

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
                _installationId.value = null
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
            isLoading = true
            errorMessage = null

            try {
                val reservesResult = withContext(Dispatchers.IO) {

                    repository?.setStatus(id, "IN_PROGRESS")

                    if (id > 0) {
                        getReservesOnce(id)
                    } else {
                        emptyList()
                    }
                }

                reserves = reservesResult

            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro inesperado"
            } finally {
                isLoading = false
            }
        }
    }

    fun insertInstallation(directExecution: DirectExecution) {
        viewModelScope.launch {
            try {
                isLoading = true
                if (directExecution.contractId != null) {
                    repository?.getDirectExecutionByContractId(directExecution.contractId!!)?.let {
                        _installationId.value = it.directExecutionId
                        return@launch
                    }
                }
                withContext(Dispatchers.IO) {
                    repository?.insertExecution(directExecution)
                }

                _installationId.value = directExecution.directExecutionId
                contractor = directExecution.description
                creationDate = directExecution.creationDate
                acceptedResponsibilityTerm = false
                contractId = directExecution.contractId

            } catch (e: Exception) {
                errorMessage = if (e.message?.lowercase()?.contains("unique") == true) {
                    "Instalação já salva anteriormente"
                } else {
                    e.message ?: ""
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun syncContracts() {
        viewModelScope.launch {
            try {
                isLoading = true
                contractRepository?.syncContracts()
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

}
