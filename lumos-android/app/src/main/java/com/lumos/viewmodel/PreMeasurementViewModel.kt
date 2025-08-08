package com.lumos.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.repository.PreMeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PreMeasurementViewModel(
    private val repository: PreMeasurementRepository,
) : ViewModel() {
    var deviceId by mutableStateOf<UUID?>(null)
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var loading by mutableStateOf(false)
    var locationLoading by mutableStateOf(false)
    private val _streets = mutableStateOf<List<PreMeasurementStreet>>(emptyList()) // estado da lista
    val streets: State<List<PreMeasurementStreet>> = _streets // estado acessível externamente

    init {
        deviceId = UUID.randomUUID()
    }

    fun saveStreetOffline(preMeasurementStreet: PreMeasurementStreet, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.saveStreet(preMeasurementStreet)
                }
                callback(result) // Passando o resultado para a função de callback
            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
                callback(null) // Retorna um valor de erro ou qualquer valor que faça sentido
            }
        }
    }

    fun saveItemsOffline(
        preMeasurementStreetItems: List<PreMeasurementStreetItem>,
        preMeasurementStreetId: Long
    ) {
//        viewModelScope.launch() {
//            loading = true
//            try {
//                withContext(Dispatchers.IO){
//                    item.preMeasurementStreetId = preMeasurementStreetId
//                    repository.saveItem(items)
//                }
//
//                finishMeasurement = true
//            } catch (e: Exception) {
//                Log.e("Error", e.message.toString())
//            }
//            finally {
//                loading = false
//            }
//        }
    }

    fun loadStreets(contractId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetched = repository.getStreets(contractId)
                _streets.value = fetched // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun queueSendMeasurement(contractId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.queueSendMeasurement(contractId)
            } catch (e: Exception) {
                Log.e("Erro view model - sendPreMeasurementSync", e.message.toString())
            }
        }
    }


}
