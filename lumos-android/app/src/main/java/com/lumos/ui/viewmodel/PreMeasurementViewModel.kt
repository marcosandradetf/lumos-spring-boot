package com.lumos.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.PreMeasurementRepository
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreet
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PreMeasurementViewModel(
    private val repository: PreMeasurementRepository,
) : ViewModel() {
    private val _streets = mutableStateOf<List<PreMeasurementStreet>>(emptyList()) // estado da lista
    val streets: State<List<PreMeasurementStreet>> = _streets // estado acessível externamente

    fun saveStreetOffline(preMeasurementStreet: PreMeasurementStreet, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.saveStreet(preMeasurementStreet)
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
        viewModelScope.launch {
            try {
                // Usando async para executar o salvamento dos itens de forma paralela
                preMeasurementStreetItems.map { item ->
                    async {
                        item.preMeasurementStreetId = preMeasurementStreetId
                        repository.saveItem(item)
                    }
                }
            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
            }
        }
    }

    fun loadStreets(contractId: Long) {
        viewModelScope.launch {
            try {
                val fetched = repository.getStreets(contractId)
                _streets.value = fetched // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun sendPreMeasurementSync(contractId: Long) {
        viewModelScope.launch {
            try {
                repository.queueSyncMeasurement(contractId)
            } catch (e: Exception) {
                Log.e("Erro view model - sendPreMeasurementSync", e.message.toString())
            }
        }
    }


}
