package com.lumos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lumos.data.repository.MeasurementRepository
import com.lumos.domain.model.Item
import com.lumos.domain.model.Measurement

class MeasurementViewModel(
    private val repository: MeasurementRepository,

) : ViewModel() {

    suspend fun saveMeasurementOffline(measurement: Measurement) {
        repository.saveMeasurement(measurement)
    }

    suspend fun saveItensOffiline(item: Item){
        repository.saveItem(item)
    }

}
