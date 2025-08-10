package com.lumos.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.repository.PreMeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PreMeasurementViewModel(
    private val repository: PreMeasurementRepository? = null,
) : ViewModel() {

    var preMeasurementId by mutableStateOf<UUID?>(null)
    var preMeasurementStreetId by mutableStateOf<UUID?>(null)

    var measurement by mutableStateOf<PreMeasurement?>(null)
    var street by mutableStateOf<PreMeasurementStreet?>(null)
    val streetItems = mutableListOf<PreMeasurementStreetItem>()


    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var loading by mutableStateOf(false)
    var locationLoading by mutableStateOf(false)

    private val _streets = mutableStateOf<List<PreMeasurementStreet>>(emptyList()) // estado da lista
    val streets: State<List<PreMeasurementStreet>> = _streets // estado acessível externamente



    var hasPosted by mutableStateOf(false)
    var alertModal by mutableStateOf(false)
    var confirmModal by mutableStateOf(false)
    var nextStep by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun newPreMeasurement(contractId: Long, contractor: String) {
        if(measurement == null) {
            preMeasurementId = UUID.randomUUID()
            measurement = PreMeasurement(
                preMeasurementId = preMeasurementId.toString(),
                contractId = contractId,
                contractor = contractor
            )
        }
        if(street == null && preMeasurementId != null) {
            preMeasurementStreetId = UUID.randomUUID()
            street = PreMeasurementStreet(
                preMeasurementStreetId = preMeasurementStreetId.toString(),
                preMeasurementId = preMeasurementId.toString(),
                lastPower = null,
                latitude = null,
                longitude = null,
                address = null,
                photoUri = null,
                status = null
            )
        }
    }

    fun addItem(itemContractId: Long) {
        streetItems.add(
            PreMeasurementStreetItem(
                preMeasurementStreetId = preMeasurementStreetId.toString(),
                preMeasurementId = preMeasurementId.toString(),
                contractReferenceItemId = itemContractId,
                measuredQuantity = "1"
            )
        )
    }

    fun removeItem(itemContractId: Long) {
        streetItems.removeAll { it.contractReferenceItemId == itemContractId }
    }

    fun setQuantity(itemContractId: Long, measuredQuantity: String) {
        streetItems.find { it.contractReferenceItemId == itemContractId }
            ?.measuredQuantity = measuredQuantity
    }



    fun save() {
        viewModelScope.launch {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    repository?.save(measurement!!, street!!, streetItems)
                }

                hasPosted = true
            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
                message = e.message ?: ""
            } finally {
                loading = false
            }
        }
    }

    fun loadStreets(preMeasurementId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetched = repository?.getStreets(preMeasurementId)
                _streets.value = fetched!! // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun queueSendMeasurement(preMeasurementId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository?.queueSendMeasurement(preMeasurementId)
            } catch (e: Exception) {
                Log.e("Erro view model - sendPreMeasurementSync", e.message.toString())
            }
        }
    }

    fun clearViewModel() {
        loading = true

        street = null
        streetItems.clear()
        hasPosted = false
        message = null
        alertModal = false
        confirmModal = false
        nextStep = false

        loading = false
    }


}
