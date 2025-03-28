package com.lumos.ui.preMeasurement

import android.util.Log
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

    fun sendPreMeasurementSync(contractId: Long) {
        viewModelScope.launch {
            try {
                repository.syncMeasurement(contractId)
            } catch (e: Exception) {
                Log.e("Erro view model - sendPreMeasurementSync", e.message.toString())
            }
        }
    }

}
