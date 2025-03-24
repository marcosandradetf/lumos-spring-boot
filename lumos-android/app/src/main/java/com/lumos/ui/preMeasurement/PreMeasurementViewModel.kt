package com.lumos.ui.preMeasurement

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.data.repository.MeasurementRepository
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class PreMeasurementViewModel(
    private val repository: MeasurementRepository,

    ) : ViewModel() {


    private val _preMeasurements = mutableStateOf<List<PreMeasurement>>(emptyList()) // estado da lista
    val preMeasurements: State<List<PreMeasurement>> = _preMeasurements // estado acessível externamente

    fun loadPreMeasurements() {
        viewModelScope.launch {
            try {
                val fetched = repository.getPreMeasurements()
                _preMeasurements.value = fetched // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun savePreMeasurementOffline(preMeasurement: PreMeasurement, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.savePreMeasurement(preMeasurement)
                callback(result) // Passando o resultado para a função de callback
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                callback(null) // Retorna um valor de erro ou qualquer valor que faça sentido
            }
        }
    }

    fun saveMeasurementOffline(preMeasurementStreet: PreMeasurementStreet, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.saveMeasurement(preMeasurementStreet)
                callback(result) // Passando o resultado para a função de callback
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                callback(null) // Retorna um valor de erro ou qualquer valor que faça sentido
            }
        }
    }

    fun saveItemsOffline(preMeasurementStreetItems: List<PreMeasurementStreetItem>, measurementId: Long) {
        viewModelScope.launch {
            try {
                // Usando async para executar o salvamento dos itens de forma paralela
                val deferredResults = preMeasurementStreetItems.map { item ->
                    async {
                        item.preMeasurementStreetId = measurementId
                        repository.saveItem(item)
                    }
                }

                // Aguarda a conclusão de todas as operações de salvamento
                deferredResults.awaitAll()

                // Chama a sincronização após salvar todos os itens
                repository.syncMeasurement()
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    suspend fun getPreMeasurement(preMeasurementId: Long): PreMeasurement {
        return repository.getPreMeasurement(preMeasurementId)
    }


}
