package com.lumos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.ReserveMaterialJoin
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
    mockContractor: String? = null,
    mockCreationDate: String? = null,
    mockStreets: List<DirectExecutionStreet> = emptyList(),

    ) : ViewModel() {
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    var installationId by mutableStateOf<Long?>(null)
    var creationDate by mutableStateOf(mockCreationDate)
    var contractId by mutableStateOf<Long?>(null)
    var contractor by mutableStateOf(mockContractor)

    var street by mutableStateOf<DirectExecutionStreet?>(null)
    var streets by mutableStateOf(mockStreets)
    var streetItems by mutableStateOf(listOf<DirectExecutionStreetItem>())

    var hasPosted by mutableStateOf(false)
    var alertModal by mutableStateOf(false)
    var confirmModal by mutableStateOf(false)
    var showFinishForm by mutableStateOf(false)
    var showSignScreen by mutableStateOf(false)

    var loadingCoordinates by mutableStateOf(false)
    var nextStep by mutableStateOf(false)
    var triedToSubmit by mutableStateOf(false)
    var sameStreet by mutableStateOf(false)

    var isLoading by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)

    var reserves by mutableStateOf<List<ReserveMaterialJoin>>(emptyList())
    var stockCount by mutableIntStateOf(0)

    var responsible by mutableStateOf<String?>(null)
    var signPath by mutableStateOf<String?>(null)
    var signDate: String? = null

    var responsibleError by mutableStateOf<String?>(null)
    var instructions by mutableStateOf<String?>(null)
    var hasResponsible by mutableStateOf<Boolean?>(null)

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

    fun clearViewModel() {
        isLoading = true

        street = null
        streetItems = emptyList()
        reserves = emptyList()
        hasPosted = false
        errorMessage = null
        alertModal = false
        confirmModal = false
        loadingCoordinates = false
        nextStep = false
        sameStreet = false

        isLoading = false
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

    fun syncExecutions() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncError.value = null
            try {
                isLoading = true
                val response = repository?.syncDirectExecutions()
                when (response) {
                    is RequestResult.Timeout -> _syncError.value =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Mas você pode continuar com o que tempos aqui — ou puxe para atualizar agora mesmo."

                    is RequestResult.NoInternet -> _syncError.value =
                        "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo."

                    is ServerError -> _syncError.value = response.message
                    is RequestResult.Success -> _syncError.value = null
                    is RequestResult.UnknownError -> _syncError.value = null
                    is RequestResult.SuccessEmptyBody -> {
                        ServerError(204, "Resposta 204 inesperada")
                    }

                    null -> null
                }
            } catch (e: Exception) {
                isLoading = false
                _syncError.value = e.message ?: "Erro inesperado."
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun getReservesOnce(directExecutionId: Long): List<ReserveMaterialJoin> {
        return withContext(Dispatchers.IO) {
            repository?.getReservesOnce(directExecutionId) ?: emptyList()
        }
    }

    fun saveAndPost(
        street: DirectExecutionStreet,
        items: List<DirectExecutionStreetItem>,
        onPostExecuted: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                withContext(Dispatchers.IO) {
                    repository?.createStreet(street.copy(finishAt = Instant.now().toString()), items)
                }
                onPostExecuted()
            } catch (e: IllegalStateException) {
                isLoading = false
                onError(e.message ?: "Erro inesperado")
            } catch (e: Exception) {
                isLoading = false
                onError("Erro: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    fun submitInstallation() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository?.queueSubmitInstallation(installationId, responsible, signPath, signDate)
                    street = null
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

    fun loadExecutionData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                initializeExecution(installationId!!, contractor!!)
                repository?.setStatus(installationId!!, "IN_PROGRESS")
                reserves = getReservesOnce(installationId!!)
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun countStock() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stockCount = repository?.countStock() ?: 0
            } catch (e: Exception) {
                errorMessage = e.message
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
                    streets = repository?.getStreets(installationId)!!
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro ao carregar as ruas da pré-medição"
            } finally {
                isLoading = false
            }
        }
    }

}
