package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.navigation.Routes
import com.lumos.repository.ContractRepository
import com.lumos.repository.PreMeasurementInstallationRepository
import com.lumos.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class PreMeasurementInstallationViewModel(
    private val repository: PreMeasurementInstallationRepository?,
    private val contractRepository: ContractRepository?,
    private val savedStateHandle: SavedStateHandle?,

    // -> Param to utilize preview
    mockStreets: List<PreMeasurementInstallationStreet> = emptyList(),
    mockItems: List<ItemView> = emptyList(),
    mockCurrentStreet: PreMeasurementInstallationStreet? = null

) : ViewModel() {
    var installationID by mutableStateOf(savedStateHandle?.get<String>("id"))
    var contractor: String? = savedStateHandle?.get<String>("contractor")
    var contractId: Long? = savedStateHandle?.get<Long>("contractId")
    var instructions by mutableStateOf(savedStateHandle?.get<String>("instructions"))
    var signPhotoUri: String? = null
    var signDate: String? = null
    var currentInstallationStreets by mutableStateOf(mockStreets)
    var currentInstallationItems by mutableStateOf(mockItems)

    var lastItem by mutableStateOf<ItemView?>(null)
    var currentStreetId: String? = null

    var currentStreet by mutableStateOf(mockCurrentStreet)
    // -> Bellow Properties to UI State
    var loading by mutableStateOf(false)

    var alertModal by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var hasPosted by mutableStateOf(false)
    var openConfirmation by mutableStateOf(false)
    var showSignScreen by mutableStateOf(false)
    var route by mutableStateOf<String?>(null)

    var responsible by mutableStateOf<String?>(null)
    var responsibleError by mutableStateOf<String?>(null)
    var hasResponsible by mutableStateOf<Boolean?>(null)
    var triedToSubmit by mutableStateOf(false)
    var showFinishForm by mutableStateOf(false)
    var buttonLoading by mutableStateOf(false)

    // -> control state viewModel
    init {
        setStreets()
        viewModelScope.launch {
            // Observa eventos de navegação enviados via SavedStateHandle
            savedStateHandle
                ?.getStateFlow("route_event", null as String?)
                ?.collect { route ->

                    when (route) {
                        Routes.INSTALLATION_HOLDER -> setStateForHolderScreen()
                        Routes.PRE_MEASUREMENT_INSTALLATION_STREETS -> {
                            setStateForStreetScreen()
                        }
                    }

                    // limpa o evento para não repetir
                    savedStateHandle["route_event"] = null
                }
        }
    }

    fun setStateForHolderScreen() {
        installationID = null
        signPhotoUri = null
        signDate = null
        contractId = null
        contractor = null
        currentInstallationStreets = emptyList()
        currentInstallationItems = emptyList()
        lastItem = null
        currentStreetId = null
        currentStreet = null
        alertModal = false
        hasPosted = false
        openConfirmation = false
        showSignScreen = false
        showFinishForm = false
        responsible = null
        signDate = null
        signPhotoUri = null
        hasResponsible = null
        triedToSubmit = false
    }

    fun setStateForStreetScreen() {
        currentInstallationItems = emptyList()
        lastItem = null
        currentStreetId = null
        currentStreet = null
        alertModal = false
        hasPosted = false
        openConfirmation = false
        showSignScreen = false
        showFinishForm = false
        responsible = null
        signDate = null
        signPhotoUri = null
        hasResponsible = null
        triedToSubmit = false
    }

    // -> Bellow properties to actions or get/sets
    fun setStreets() {
        viewModelScope.launch {
            loading = true
            message = null
            try {
                withContext(Dispatchers.IO) {
                    repository?.setInstallationStatus(installationID ?: "")
                    currentInstallationStreets = repository?.getStreets(installationID)!!
                }
            } catch (e: Exception) {
                message = e.message ?: "Erro ao carregar as ruas da pré-medição"
                Utils.sendLog("premeasurementinstallationviewmodel", "setStreets", e.message)
            } finally {
                loading = false
            }
        }
    }

    fun setStreetAndItems(paramCurrentStreetId: String) {
        viewModelScope.launch {
            loading = true
            message = null
            try {
                currentStreetId = paramCurrentStreetId
                currentStreet =
                    currentInstallationStreets.find { it.preMeasurementStreetId == paramCurrentStreetId }

                withContext(Dispatchers.IO) {
                    repository?.setStreetStatus(paramCurrentStreetId, "IN_PROGRESS")
                    if (contractRepository?.checkBalance() == true) {
                        contractId?.let {
                            contractRepository.getContractItemBalance(it)
                        }
                    }
                    repository?.getItems(paramCurrentStreetId)
                        ?.let { currentInstallationItems = it }
                }
            } catch (e: Exception) {
                Utils.sendLog("premeasurementinstallationviewmodel", "setstreetanditems", e.message)
                message = e.message ?: "Erro ao carregar as ruas da pré-medição"
                loading = false
            }
        }
    }

    fun setInstallationItemQuantity(
        quantityExecuted: String,
        materialStockId: Long,
        contractItemId: Long
    ) {
        viewModelScope.launch {
            buttonLoading = true
            try {
                lastItem = currentInstallationItems.find { it.materialStockId == materialStockId }?.copy(executedQuantity = quantityExecuted)

                subtractCurrentBalance(contractItemId, quantityExecuted)
                currentInstallationItems = currentInstallationItems.filter { it.materialStockId != materialStockId }

                withContext(Dispatchers.IO) {
                    repository?.setInstallationItemQuantity(
                        currentStreetId,
                        materialStockId,
                        quantityExecuted
                    )
                }

                message = "Item concluído com sucesso"
            } catch (e: Exception) {
                Utils.sendLog("premeasurementinstallationviewmodel", "setInstallationItemQuantity", e.message)
                message = e.message ?: ""
            } finally {
                buttonLoading = false
            }
        }
    }

    fun submitStreet() {
        viewModelScope.launch {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    repository?.queueSubmitStreet(currentStreet)
                }
                currentInstallationStreets =
                    currentInstallationStreets.filter { it.preMeasurementStreetId != currentStreetId }
                hasPosted = true
                showFinishForm = false
                currentStreetId = null
                currentStreet = null
            } catch (e: Exception) {
                Utils.sendLog("premeasurementinstallationviewmodel", "submitStreet", e.message)
                message = e.message ?: ""
            } finally {
                loading = false
            }
        }
    }

    fun submitInstallation() {
        viewModelScope.launch {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    repository?.queueSubmitInstallation(installationID, signPhotoUri, signDate)
                }
                installationID = null
                signPhotoUri = null
                signDate = null
                contractor = null
                contractId = null
                showFinishForm = false
            } catch (e: IllegalStateException) {
                message = e.message
            } catch (e: Exception) {
                Utils.sendLog("premeasurementinstallationviewmodel", "submitInstallation", e.message)
                message = e.message
            } finally {
                loading = false
            }
        }
    }

    fun refreshUrlImage() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    repository?.updateObjectPublicUrl(
                        currentStreetId ?: "",
                        currentStreet?.objectUri ?: ""
                    )
                }

                if (response is RequestResult.Success) {
                    currentStreet = currentStreet?.copy(photoUrl = response.data)
                }
            } catch (e: Exception) {
                Utils.sendLog("premeasurementinstallationviewmodel", "refreshUrlImage", e.message)
            }
        }
    }

    private fun subtractCurrentBalance(contractItemId: Long, quantityExecuted: String) {
        currentInstallationItems = currentInstallationItems.map { item ->
            if (item.contractItemId == contractItemId) {
                item.copy(
                    currentBalance = (BigDecimal(item.currentBalance) - BigDecimal(
                        quantityExecuted
                    )).toString()
                )
            } else item
        }
    }

    fun sumCurrentBalance(contractItemId: Long, quantityExecuted: String) {
        currentInstallationItems = currentInstallationItems.map { item ->
            if (item.contractItemId == contractItemId) {
                item.copy(
                    currentBalance = (BigDecimal(item.currentBalance) + BigDecimal(
                        quantityExecuted
                    )).toString()
                )
            } else item
        }
    }

}
