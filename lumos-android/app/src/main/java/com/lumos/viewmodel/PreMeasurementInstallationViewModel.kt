package com.lumos.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.api.RequestResult
import com.lumos.api.RequestResult.ServerError
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.repository.PreMeasurementInstallationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreMeasurementInstallationViewModel(
    private val repository: PreMeasurementInstallationRepository,

    // -> Param to utilize preview
    mockStreets: List<PreMeasurementInstallationStreet> = emptyList()

) : ViewModel() {
    // -> Bellow Properties to control installations
    var installationID: String? = null
    val contractor: String? = null
    val installationStreets = mutableStateOf(mockStreets)

    // -> Bellow Properties to UI State
    private val _loading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // -> Bellow properties to actions or get/sets
    fun setStreets() {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                installationStreets.value = repository.getStreets(installationID)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao carregar as ruas da pré-medição"
            } finally {
                _loading.value = false
            }
        }
    }

    fun syncExecutions() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _errorMessage.value = null
            try {
                val response = repository.syncExecutions()
                when (response) {
                    is RequestResult.Timeout -> _errorMessage.value =
                        "A internet está lenta e não conseguimos buscar os dados mais recentes. Mas você pode continuar com o que tempos aqui - ou puxe para atualizar agora mesmo."

                    is RequestResult.NoInternet -> _errorMessage.value =
                        "Você já pode começar com o que temos por aqui! Assim que a conexão voltar, buscamos o restante automaticamente — ou puxe para atualizar agora mesmo."

                    is ServerError -> _errorMessage.value = response.message
                    is RequestResult.Success -> _errorMessage.value = null
                    is RequestResult.UnknownError -> _errorMessage.value = null
                    is RequestResult.SuccessEmptyBody -> {
                        ServerError(204, "Resposta 204 inesperada")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro inesperado."
            } finally {
                _loading.value = false
            }
        }
    }


    fun setExecutionStatus(streetId: String, status: String) {
        viewModelScope.launch {
            try {
                repository.setExecutionStatus(streetId, status)
            } catch (e: Exception) {
                Log.e("Error setExecutionStatus", e.message.toString())
            }
        }
    }


    fun setPhotoUri(photoUri: String, streetId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.setPhotoUri(photoUri, streetId)
                }
            } catch (e: Exception) {
                Log.e("Error setPhotoUri", e.message.toString())
            }
        }
    }


//    fun finishAndCheckPostExecution(
//        reserveId: Long,
//        quantityExecuted: Double,
//        streetId: Long,
//        context: Context,
//        hasPosted: Boolean,
//        onReservesUpdated: (List<IndirectReserve>) -> Unit,
//        onPostExecuted: () -> Unit,
//        onError: (String) -> Unit = {}
//    ) {
//        viewModelScope.launch {
//            _isLoadingReserves.value = true
//            try {
//                val reserves = withContext(Dispatchers.IO) {
//                    repository.finishMaterial(reserveId, quantityExecuted)
//                    repository.getReservesOnce(streetId)
//                }
//
//                if (!hasPosted && reserves.isEmpty()) {
//                    try {
//                        withContext(Dispatchers.IO) {
//                            repository.queuePostExecution(streetId)
//                        }
//                        onPostExecuted()
//                    } catch (e: Exception) {
//                        Log.e("ViewModel", "Erro ao enviar execução", e)
//                        onError("Erro ao enviar execução: ${e.localizedMessage}")
//                    }
//                } else {
//                    onReservesUpdated(reserves)
//                }
//
//            } catch (e: Exception) {
//                Log.e("ViewModel", "Erro ao finalizar material ou buscar dados", e)
//                onError("Erro ao finalizar material: ${e.localizedMessage}")
//            } finally {
//                _isLoadingReserves.value = false
//            }
//        }
//    }



}
