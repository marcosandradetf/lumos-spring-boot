package com.lumos.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.Item
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.navigation.Routes
import com.lumos.repository.PreMeasurementRepository
import com.lumos.utils.NavEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID

class PreMeasurementViewModel(
    private val repository: PreMeasurementRepository? = null,
) : ViewModel() {

    var preMeasurementId by mutableStateOf<UUID?>(null)
    var preMeasurementStreetId by mutableStateOf<UUID?>(null)

    var measurement by mutableStateOf<PreMeasurement?>(null)
    var street by mutableStateOf<PreMeasurementStreet?>(null)

    var streetItems by mutableStateOf<List<PreMeasurementStreetItem>>(emptyList())


    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var loading by mutableStateOf(false)
    var locationLoading by mutableStateOf(false)

    var measurements by mutableStateOf<List<PreMeasurement>>(emptyList())
    private val _streets =
        mutableStateOf<List<PreMeasurementStreet>>(emptyList()) // estado da lista
    val streets: State<List<PreMeasurementStreet>> = _streets // estado acessível externamente


    var hasPosted by mutableStateOf(false)
    var alertModal by mutableStateOf(false)
    var confirmModal by mutableStateOf(false)
    var nextStep by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            NavEvents.route.collect { route ->
                when (route) {
                    Routes.CONTRACT_SCREEN, Routes.PRE_MEASUREMENTS -> {
                        preMeasurementId = null
                        measurement = null
                        nextStep = false
                        hasPosted = false
                    }

                    Routes.PRE_MEASUREMENT_PROGRESS -> {
                        preMeasurementStreetId = null
                        street = null
                        streetItems = emptyList()
                    }
                }
            }
        }
    }

    fun newPreMeasurementStreet() {
        if (street == null && preMeasurementId != null) {
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

    fun addItem(item: Item, items: List<Item>) {
        var measuredQuantity = "1"

        when (item.type) {
            "LED" -> {
                val serviceId =
                    items.find { it.type == "SERVIÇO" && it.itemDependency == item.type }?.contractReferenceItemId
                val projectId =
                    items.find { it.type == "PROJETO" && it.itemDependency == item.type }?.contractReferenceItemId

                var pMessage = ""

                if (!streetItems.any { it.contractReferenceItemId == serviceId }) {
                    pMessage = "Este item pode precisar que adicione Serviço de LED"
                }

                if (!streetItems.any { it.contractReferenceItemId == projectId }) {
                    if (pMessage.isBlank())
                        pMessage += "Este item pode precisar que adicione Projeto"
                    else
                        pMessage += " e Projeto"
                }

                message = pMessage


            }

            "RELÉ" -> {
                val ledsIds = items.filter { it.type == "LED" }.map { it.contractReferenceItemId }
                measuredQuantity = streetItems
                    .filter { ledsIds.contains(it.contractReferenceItemId) }
                    .map { it.measuredQuantity }
                    .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                    .toString()


            }

            "SERVIÇO" -> {
                if (item.itemDependency == "LED") {
                    val ledsIds =
                        items.filter { it.type == "LED" }.map { it.contractReferenceItemId }
                    measuredQuantity = streetItems
                        .filter { ledsIds.contains(it.contractReferenceItemId) }
                        .map { it.measuredQuantity }
                        .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                        .toString()


                } else {
                    val armIds =
                        items.filter { it.type == "BRAÇO" }.map { it.contractReferenceItemId }
                    measuredQuantity = streetItems
                        .filter { armIds.contains(it.contractReferenceItemId) }
                        .map { it.measuredQuantity }
                        .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                        .toString()
                }
            }

            "PROJETO" -> {
                val ledsIds = items.filter { it.type == "LED" }.map { it.contractReferenceItemId }
                measuredQuantity = streetItems
                    .filter { ledsIds.contains(it.contractReferenceItemId) }
                    .map { it.measuredQuantity }
                    .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                    .toString()
            }

            "CABO" -> {
                val arms = items.filter {  it.type == "BRAÇO" }

                val arm1Quantity = streetItems
                    .filter {
                        val x = arms.filter { it.linking?.startsWith("1") == true }.map { it.contractReferenceItemId }
                        x.contains(it.contractReferenceItemId)
                    }
                    .map { it.measuredQuantity }
                    .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                    .toString()
                

                val arm2 = arms.filter { it.linking?.startsWith("2") == true }
                val arm3 = arms.filter { it.linking?.startsWith("3") == true }

            }

        }

        streetItems = streetItems + PreMeasurementStreetItem(
            preMeasurementStreetId = preMeasurementStreetId.toString(),
            preMeasurementId = preMeasurementId.toString(),
            contractReferenceItemId = item.contractReferenceItemId,
            measuredQuantity = if (measuredQuantity == "0") "1" else measuredQuantity
        )

    }

    fun removeItem(item: Item) {
        streetItems = streetItems.filterNot { it.contractReferenceItemId == item.contractReferenceItemId }
    }

    fun setQuantity(itemContractId: Long, measuredQuantity: String) {
        streetItems = streetItems.map { item ->
            if (item.contractReferenceItemId == itemContractId) {
                item.copy(measuredQuantity = measuredQuantity)
            } else item
        }
    }

    fun save() {
        viewModelScope.launch {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    repository?.save( street!!, streetItems)
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

    fun loadStreets() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetched = repository?.getStreets(preMeasurementId.toString())
                _streets.value = fetched!! // atualiza o estado com os dados obtidos
            } catch (e: Exception) {
                Log.e("Error loadMaterials", e.message.toString())
            }
        }
    }

    fun queueSendMeasurement() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loading = true
                withContext(Dispatchers.IO) {
                    repository?.queueSendMeasurement(preMeasurementId.toString())
                }

                preMeasurementId = null
                measurement = null
            } catch (e: Exception) {
                Log.e("Erro view model - sendPreMeasurementSync", e.message.toString())
            } finally {
                loading = false
            }
        }
    }

    fun startPreMeasurement(
        contractId: Long? = null,
        contractor: String? = null,
        currentPreMeasurementId: String? = null
    ) {
        viewModelScope.launch {
            val newPreMeasurementId = UUID.randomUUID()

            try {
                loading = true
                if (contractId != null && contractor != null) {
                    val exists = repository?.existsPreMeasurementByContractId(contractId)
                    if (exists != null) {
                        preMeasurementId = UUID.fromString(exists.preMeasurementId)
                        measurement = exists
                    } else {
                        measurement = PreMeasurement(
                            preMeasurementId = newPreMeasurementId.toString(),
                            contractId = contractId,
                            contractor = contractor,
                        )
                        repository?.saveNewPreMeasurement(measurement!!)
                        preMeasurementId = newPreMeasurementId
                    }
                } else if (currentPreMeasurementId != null) {
                    measurement = repository?.getPreMeasurement(currentPreMeasurementId)
                    preMeasurementId = UUID.fromString(currentPreMeasurementId)
                }

            } catch (e: Exception) {
                message = if (e.message?.lowercase()?.contains("unique") == true) {
                    "Pré-medição já salva anteriormente"
                } else {
                    e.message
                }
            } finally {
                loading = false
            }
        }
    }

    fun loadPreMeasurements() {
        viewModelScope.launch {
            loading = true
            try {
                withContext(Dispatchers.IO) {
                    measurements = repository?.getPreMeasurements() ?: emptyList()
                }
            } catch (e: Exception) {
                message = e.message
            } finally {
                loading = false
            }
        }
    }


}
