package com.lumos.ui.measurement

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lumos.data.repository.MeasurementRepository
import com.lumos.domain.model.Item
import com.lumos.domain.model.Measurement
import com.lumos.domain.service.SyncMeasurement
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MeasurementViewModel(
    private val repository: MeasurementRepository,

    ) : ViewModel() {


    fun saveMeasurementOffline(measurement: Measurement, callback: (Long?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.saveMeasurement(measurement)
                callback(result) // Passando o resultado para a função de callback
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
                callback(null) // Retorna um valor de erro ou qualquer valor que faça sentido
            }
        }
    }

    fun saveItensOffline(items: List<Item>, measurementId: Long) {
        viewModelScope.launch {
            try {
                // Usando async para executar o salvamento dos itens de forma paralela
                val deferredResults = items.map { item ->
                    async {
                        item.measurementId = measurementId
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


}
