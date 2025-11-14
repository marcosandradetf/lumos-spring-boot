package com.lumos.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.domain.model.DirectExecution
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
    private val contractRepository: ContractRepository?

) : ViewModel() {
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    var street by mutableStateOf<DirectExecutionStreet?>(null)
    var streetItems by mutableStateOf(listOf<DirectExecutionStreetItem>())

    var hasPosted by mutableStateOf(true)
    var alertModal by mutableStateOf(false)
    var confirmModal by mutableStateOf(false)

    var loadingCoordinates by mutableStateOf(false)
    var nextStep by mutableStateOf(true)
    var sameStreet by mutableStateOf(false)

    var isLoading by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)

    var reserves by mutableStateOf<List<ReserveMaterialJoin>>(emptyList())
    var stockCount by mutableIntStateOf(0)

    var responsible: String? = null
    var signPath: String? = null
    var signDate: String? = null

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


//    fun setExecutionStatus(streetId: Long, status: String) {
//        viewModelScope.launch {
//            try {
//                repository.setExecutionStatus(streetId, status)
//            } catch (e: Exception) {
//                Log.e("Error setExecutionStatus", e.message.toString())
//            }
//        }
//    }


    suspend fun getExecution(contractId: Long): DirectExecution? {
        return withContext(Dispatchers.IO) {
            try {
                repository?.getExecution(contractId)
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                null  // Retorna null em caso de erro
            }
        }
    }

//    fun queueSyncStartExecution(streetId: Long, context: Context) {
//        viewModelScope.launch {
//            try {
//                withContext(Dispatchers.IO) {
//                    repository.queueSyncStartExecution(streetId)
//                }
//            } catch (e: Exception) {
//                Log.e("Error queueSyncFetchReservationStatus", e.message.toString())
//            }
//        }
//    }

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

    fun markAsFinished(directExecutionId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository?.markAsFinished(directExecutionId, responsible, signPath, signDate)
                    street = null
                }
            } catch (e: IllegalStateException) {
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun loadExecutionData(directExecutionId: Long, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                initializeExecution(directExecutionId, description)
                repository?.setStatus(directExecutionId, "IN_PROGRESS")
                reserves = getReservesOnce(directExecutionId)
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

}
