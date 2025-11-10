package com.lumos.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.domain.model.ItemView
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.navigation.Routes
import com.lumos.repository.ContractRepository
import com.lumos.repository.PreMeasurementInstallationRepository
import com.lumos.utils.NavEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreMeasurementInstallationViewModel(
    private val repository: PreMeasurementInstallationRepository?,
    private val contractRepository: ContractRepository?,

    // -> Param to utilize preview
    mockStreets: List<PreMeasurementInstallationStreet> = emptyList(),
    mockItems: List<ItemView> = emptyList(),
    mockCurrentStreet: PreMeasurementInstallationStreet? = null

) : ViewModel() {

    // -> Bellow Properties to control installations
    var installationID: String? = null
    var signPhotoUri: String? = null
    var signDate: String? = null
    var contractor: String? = null
    var currentInstallationStreets by mutableStateOf(mockStreets)
    var currentInstallationItems by mutableStateOf(mockItems)
    var lastItem by mutableStateOf<ItemView?>(null)

    var currentStreetId: String? = null
    var currentStreet by mutableStateOf(mockCurrentStreet)

    // -> Bellow Properties to UI State
    var loading by mutableStateOf(false)
    var alertModal by mutableStateOf(false)
    var showExpanded by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var hasPosted by mutableStateOf(false)
    var checkBalance by mutableStateOf(false)
    var openConfirmation by mutableStateOf(false)

    // -> control viewModel

    init {
        viewModelScope.launch {
            NavEvents.route.collect { route ->
                when (route) {
                    Routes.INSTALLATION_HOLDER -> {
                        installationID = null
                        contractor = null
                        currentStreetId = null
                        currentStreet = null
                        currentInstallationStreets = emptyList()
                        currentInstallationItems = emptyList()
                    }

                    Routes.PRE_MEASUREMENT_INSTALLATION_STREETS -> {
                        currentStreetId = null
                        currentStreet = null
                        currentInstallationItems = emptyList()
                    }
                }
            }
        }
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
                    if(contractRepository?.getContractItemBalance() == RequestResult.Success(Unit)) checkBalance = true
                    currentInstallationItems = repository?.getItems(paramCurrentStreetId)!!
                }
            } catch (e: Exception) {
                message = e.message ?: "Erro ao carregar as ruas da pré-medição"
            } finally {
                loading = false

            }
        }
    }

    fun syncExecutions() {
        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            message = null
            try {
                val response = repository?.syncExecutions()!!
                when (response) {
                    is RequestResult.Timeout -> message =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Mas você pode continuar com o que tempos aqui - ou puxe para atualizar agora mesmo."

                    is RequestResult.NoInternet -> message =
                        "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo."

                    is ServerError -> message = response.message
                    is RequestResult.Success -> message = null
                    is RequestResult.UnknownError -> message = null
                    is RequestResult.SuccessEmptyBody -> {
                        ServerError(204, "Resposta 204 inesperada")
                    }
                }
            } catch (e: Exception) {
                message = e.message ?: "Erro inesperado."
            } finally {
                loading = false
            }
        }
    }


    fun setExecutionStatus(streetId: String, status: String) {
        viewModelScope.launch {
            try {
                repository?.setStreetStatus(streetId, status)
            } catch (e: Exception) {
                Log.e("Error setExecutionStatus", e.message.toString())
            }
        }
    }


    fun setPhotoUri(photoUri: String, streetId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository?.setPhotoUri(photoUri, streetId)
                }
            } catch (e: Exception) {
                Log.e("Error setPhotoUri", e.message.toString())
            }
        }
    }

    fun hasPhotoUrl(): Boolean {
        return currentStreet?.photoExpiration != null
    }

    fun isPhotoUrlExpired(): Boolean {
        val now = System.currentTimeMillis() / 1000
        return now >= (currentStreet?.photoExpiration ?: 0L)
    }

    fun setInstallationItemQuantity(quantityExecuted: String, materialStockId: Long) {
        viewModelScope.launch {
            loading = true
            try {
                lastItem = currentInstallationItems.find { it.materialStockId == materialStockId }
                currentInstallationItems =
                    currentInstallationItems.filter { it.materialStockId != materialStockId }
                withContext(Dispatchers.IO) {
                    repository?.setInstallationItemQuantity(
                        currentStreetId,
                        materialStockId,
                        quantityExecuted
                    )
                }
                message = "Item concluído com sucesso"
            } catch (e: Exception) {
                message = e.message ?: ""
            } finally {
                loading = false
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
                currentStreetId = null
                currentStreet = null
            } catch (e: Exception) {
                message = e.message ?: ""
            } finally {
                loading = false
            }
        }
    }

    fun submitInstallation() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository?.queueSubmitInstallation(installationID, signPhotoUri, signDate)
                }
                installationID = null
                signPhotoUri = null
                signDate = null
            } catch (e: IllegalStateException) {
                null
            } catch (e: Exception) {
                null
            }
        }
    }

}
