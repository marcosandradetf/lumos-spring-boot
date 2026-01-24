package com.lumos.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumos.domain.model.Item
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.service.CoordinatesService
import com.lumos.navigation.Routes
import com.lumos.repository.PreMeasurementRepository
import com.lumos.utils.NavEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID

class PreMeasurementViewModel(
    private val repository: PreMeasurementRepository? = null,
    savedStateHandle: SavedStateHandle,
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
    var streets by mutableStateOf<List<PreMeasurementStreet>>(emptyList())

    var autoCalculate by mutableStateOf(false)

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
                        streets = emptyList()
                    }

                    Routes.PRE_MEASUREMENT_PROGRESS -> {
                        preMeasurementStreetId = null
                        street = null
                        hasPosted = false
                        streetItems = emptyList()
                        nextStep = false
                    }
                }
            }
        }

        autoCalculate = repository?.getAutoCalculate() ?: false
    }

    fun toggleAutoCalculate(items: List<Item>) {
        autoCalculate = !autoCalculate
        repository?.toggleAutoCalculate(autoCalculate)

        if(autoCalculate) {
            message = "Opção de cálculo automático ativado"

            calculateCable(items, null, "-1")
            calculateRelay(items, null, "-1")
            calculateArmService(items, null, "-1")
            calculateLedService(items, null, "-1")
            calculateProject(items, null, "-1")
        } else message = "Opção de cálculo automático desativado"


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
        calculateQuantity(item, items)
    }

    private fun addUpdateOrRemoveItem(items: List<Item>? = null, item: Item, measuredQuantity: String = "1") {
        when (measuredQuantity) {
            "1" -> {
                streetItems = streetItems + PreMeasurementStreetItem(
                    preMeasurementStreetId = preMeasurementStreetId.toString(),
                    preMeasurementId = preMeasurementId.toString(),
                    contractReferenceItemId = item.contractReferenceItemId,
                    measuredQuantity = measuredQuantity
                )
            }

            "-1" -> {
                streetItems = streetItems.filterNot { it.contractReferenceItemId == item.contractReferenceItemId }
            }

            else -> {
                streetItems = streetItems.map { si ->
                    if (si.contractReferenceItemId == item.contractReferenceItemId) {
                        si.copy(measuredQuantity = measuredQuantity)
                    } else si
                }
            }
        }

        when (item.type) {
            "LED" -> {
                calculateRelay(items!!, null, "-1")
                calculateLedService(items, null, "-1")
                calculateProject(items, null, "-1")
            }

            "REFLETOR" -> {
                calculateLedService(items!!, null, "-1")
            }

            "BRAÇO" -> {
                calculateCable(items!!, null, "-1")
                calculateArmService(items, null, "-1")
            }
        }
    }

    fun calculateQuantity(item: Item, items: List<Item>, measuredQuantity: String = "1") {
        when (item.type) {
            "LED" -> {
                addUpdateOrRemoveItem(items, item, measuredQuantity)

                if (measuredQuantity == "1") {
                    val serviceId =
                        items.find { it.type == "SERVIÇO" && it.itemDependency == item.type }?.contractReferenceItemId
                    val projectId =
                        items.find { it.type == "PROJETO" && it.itemDependency == item.type }?.contractReferenceItemId
                    val relayId =
                        items.find { it.type == "RELÉ" }?.contractReferenceItemId

                    var pMessage = ""

                    viewModelScope.launch {
                        while(message != null) {
                            delay(100)
                        }

                        if (!streetItems.any { it.contractReferenceItemId == serviceId }) {
                            pMessage = "Adicionar este item pode exigir a inclusão de Serviço de Instalação de LEDS"
                        }

                        if (!streetItems.any { it.contractReferenceItemId == projectId }) {
                            pMessage += if (pMessage.isBlank())
                                "Adicionar este item pode exigir a inclusão de serviço de Projeto por IP"
                            else if (!streetItems.any { it.contractReferenceItemId == relayId })
                                ", Projeto por IP"
                            else
                                " e Projeto por IP"
                        }

                        if (!streetItems.any { it.contractReferenceItemId == relayId }) {
                            pMessage += if (pMessage.isBlank())
                                "Adicionar este item pode exigir a inclusão de Relé"
                            else
                                " e Relé"
                        }

                        if(pMessage.isNotBlank()) message = pMessage
                    }
                }
            }

            "REFLETOR" -> {
                addUpdateOrRemoveItem(items, item, measuredQuantity)

                if (measuredQuantity == "1") {
                    val serviceId =
                        items.find { it.type == "SERVIÇO" && it.itemDependency == item.type }?.contractReferenceItemId

                    var pMessage = ""

                    viewModelScope.launch {
                        while(message != null) {
                            delay(100)
                        }

                        if (!streetItems.any { it.contractReferenceItemId == serviceId }) {
                            pMessage = "Adicionar este item pode exigir a inclusão de Serviço de Instalação de LEDS"
                        }

                        if(pMessage.isNotBlank()) message = pMessage
                    }
                }
            }

            "RELÉ" -> {
                calculateRelay(items, item.contractReferenceItemId, measuredQuantity)
            }

            "SERVIÇO" -> {
                if (item.itemDependency == "LED") {
                    calculateLedService(items, listOf(item.contractReferenceItemId), measuredQuantity)
                } else {
                    calculateArmService(items, item.contractReferenceItemId, measuredQuantity)
                }
            }

            "PROJETO" -> {
                calculateProject(items, listOf(item.contractReferenceItemId), measuredQuantity)
            }

            "CABO" -> {
                calculateCable(items, item.contractReferenceItemId, measuredQuantity)
            }

            "BRAÇO" -> {
                addUpdateOrRemoveItem(items, item, measuredQuantity)

                if (measuredQuantity != "1") return

                val serviceId =
                    items.find { it.type == "SERVIÇO" && it.itemDependency == item.type }?.contractReferenceItemId

                val cableIds = items
                    .filter { it.type == "CABO" }
                    .map { it.contractReferenceItemId }

                viewModelScope.launch {
                    while(message != null) {
                        delay(100)
                    }

                    if (!streetItems.any { it.contractReferenceItemId == serviceId }) {
                        message =
                            "Adicionar este item pode exigir a inclusão do Serviço 'Troca de Ponto'"
                    }

                    if (!streetItems.any { cableIds.contains(it.contractReferenceItemId) }) {
                        if (message == null) message =
                            "Adicionar este item pode exigir a inclusão de Cabo"
                        else message += " e o item Cabo"
                    }
                }

            }

            else -> addUpdateOrRemoveItem(null, item, measuredQuantity)
        }


    }

    private fun calculateRelay(
        items: List<Item>,
        relayId: Long? = null,
        manuallyQuantity: String = "1"
    ) {
        val relayId = relayId ?: items.find { it.type == "RELÉ" }?.contractReferenceItemId
        var autoQuantity = manuallyQuantity

        if (autoCalculate) {
            val ledsIds = items.filter { it.type == "LED" }.map { it.contractReferenceItemId }
            autoQuantity = streetItems
                .filter { ledsIds.contains(it.contractReferenceItemId) }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .toString()
        }

        if (streetItems.any { it.contractReferenceItemId == relayId }) {

            if(listOf("0", "0.0").contains(autoQuantity) && autoCalculate) {
                streetItems = streetItems.filterNot { si -> relayId == si.contractReferenceItemId }

                message = "Relé removido automaticamente"
            } else {
                streetItems = streetItems.map { si ->
                    if (relayId == si.contractReferenceItemId) {
                        si.copy(measuredQuantity = autoQuantity)
                    } else si
                }

                if (autoCalculate) message = "Quantidade de relé definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
            }

        } else if (manuallyQuantity != "-1") {
            streetItems = streetItems + PreMeasurementStreetItem(
                preMeasurementStreetId = preMeasurementStreetId.toString(),
                preMeasurementId = preMeasurementId.toString(),
                contractReferenceItemId = relayId ?: 0,
                measuredQuantity = if (autoQuantity == "0") manuallyQuantity else autoQuantity
            )

            if (autoCalculate) message = "Quantidade de relé definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
        }

    }


    private fun calculateCable(
        items: List<Item>,
        pCablesIds: Long? = null,
        manuallyQuantity: String = "1"
    ) {
        val cablesIds =
            if (pCablesIds != null) listOf(pCablesIds)
            else items
                .filter { it.type == "CABO" }
                .map { it.contractReferenceItemId }

        var autoQuantity = manuallyQuantity

        if (autoCalculate) {
            val arms = items.filter { it.type == "BRAÇO" }

            val arm1Quantity = streetItems
                .filter {
                    val x = arms
                        .filter { a -> a.linking?.startsWith("1") == true }
                        .map { a -> a.contractReferenceItemId }
                    x.contains(it.contractReferenceItemId)
                }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .multiply(BigDecimal("6.5"))

            val arm2Quantity = streetItems
                .filter {
                    val x = arms
                        .filter { a -> a.linking?.startsWith("2") == true }
                        .map { a -> a.contractReferenceItemId }
                    x.contains(it.contractReferenceItemId)
                }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .multiply(BigDecimal("9.5"))

            val arm3Quantity = streetItems
                .filter {
                    val x = arms
                        .filter { a -> a.linking?.startsWith("3") == true }
                        .map { a -> a.contractReferenceItemId }
                    x.contains(it.contractReferenceItemId)
                }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .multiply(BigDecimal("12.5"))

            autoQuantity = arm1Quantity.plus(arm2Quantity).plus(arm3Quantity).toString()
        }

        if (streetItems.any { cablesIds.contains(it.contractReferenceItemId) }) {
            if(listOf("0", "0.0").contains(autoQuantity) && autoCalculate) {
                streetItems = streetItems.filterNot { si -> cablesIds.contains(si.contractReferenceItemId) }

                message = "Cabos removidos automaticamente"
            } else {
                streetItems = streetItems.map { si ->
                    if (cablesIds.contains(si.contractReferenceItemId)) {
                        si.copy(measuredQuantity = autoQuantity)
                    } else si
                }

                if (autoCalculate) message = "Quantidade de cabos definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
            }

        } else if (manuallyQuantity != "-1") {
            cablesIds
                .map { ci ->
                    streetItems = streetItems + PreMeasurementStreetItem(
                        preMeasurementStreetId = preMeasurementStreetId.toString(),
                        preMeasurementId = preMeasurementId.toString(),
                        contractReferenceItemId = ci,
                        measuredQuantity = if (autoQuantity == "0") manuallyQuantity else autoQuantity
                    )
                }

            if (autoCalculate) message =
                "Quantidade de cabos definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
        }

    }

    private fun calculateLedService(
        items: List<Item>,
        pServicesIds: List<Long>? = null,
        manuallyQuantity: String = "1"
    ) {
        val servicesIds = pServicesIds
            ?: items.filter { (it.type == "SERVIÇO" && it.itemDependency == "LED") }
                .map { it.contractReferenceItemId }

        var autoQuantity = manuallyQuantity

        if (autoCalculate) {
            val ledsIds = items.filter { it.type == "LED" || it.type == "REFLETOR" }.map { it.contractReferenceItemId }
            autoQuantity = streetItems
                .filter { ledsIds.contains(it.contractReferenceItemId) }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .toString()
        }

        if (streetItems.any { servicesIds.contains(it.contractReferenceItemId) }) {
            if(listOf("0", "0.0").contains(autoQuantity) && autoCalculate) {
                streetItems = streetItems.filterNot { si -> servicesIds.contains(si.contractReferenceItemId) }

                message = "Serviço de instalação de LEDS removido automaticamente"
            } else {
                streetItems = streetItems.map { si ->
                    if (servicesIds.contains(si.contractReferenceItemId)) {
                        si.copy(measuredQuantity = autoQuantity)
                    } else si
                }

                if(autoCalculate) message = "Valor do serviço de instalação de LEDS definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
            }
        } else if (manuallyQuantity != "-1")  {
            servicesIds.map {
                streetItems = streetItems + PreMeasurementStreetItem(
                    preMeasurementStreetId = preMeasurementStreetId.toString(),
                    preMeasurementId = preMeasurementId.toString(),
                    contractReferenceItemId = it,
                    measuredQuantity = if (autoQuantity == "0") manuallyQuantity else autoQuantity
                )
            }
            if(autoCalculate) message = "Valor do serviço de instalação de LEDS definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
        }

    }

    private fun calculateProject(
        items: List<Item>,
        pServicesIds: List<Long>? = null,
        manuallyQuantity: String = "1"
    ) {
        val servicesIds = pServicesIds
            ?: items.filter { it.type == "PROJETO" }
                .map { it.contractReferenceItemId }

        var autoQuantity = manuallyQuantity

        if (autoCalculate) {
            val ledsIds = items.filter { it.type == "LED" }.map { it.contractReferenceItemId }
            autoQuantity = streetItems
                .filter { ledsIds.contains(it.contractReferenceItemId) }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .toString()
        }

        if (streetItems.any { servicesIds.contains(it.contractReferenceItemId) }) {
            if(listOf("0", "0.0").contains(autoQuantity) && autoCalculate) {
                streetItems = streetItems.filterNot { si -> servicesIds.contains(si.contractReferenceItemId) }

                message = "Serviço projeto por IP removido automaticamente"
            } else {
                streetItems = streetItems.map { si ->
                    if (servicesIds.contains(si.contractReferenceItemId)) {
                        si.copy(measuredQuantity = autoQuantity)
                    } else si
                }

                if(autoCalculate) message = "Valor do serviço projeto por IP definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
            }
        } else if (manuallyQuantity != "-1")  {
            servicesIds.map {
                streetItems = streetItems + PreMeasurementStreetItem(
                    preMeasurementStreetId = preMeasurementStreetId.toString(),
                    preMeasurementId = preMeasurementId.toString(),
                    contractReferenceItemId = it,
                    measuredQuantity = if (autoQuantity == "0") manuallyQuantity else autoQuantity
                )
            }
            if(autoCalculate) message = "Valor do serviço projeto por IP definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
        }

    }

    private fun calculateArmService(
        items: List<Item>,
        pServiceId: Long? = null,
        manuallyQuantity: String = "1"
    ) {
        val serviceId = pServiceId
            ?: items.find { (it.type == "SERVIÇO" && it.itemDependency == "BRAÇO") }?.contractReferenceItemId

        var autoQuantity = manuallyQuantity

        if (autoCalculate) {
            val armsIds = items.filter { it.type == "BRAÇO" }.map { it.contractReferenceItemId }
            autoQuantity = streetItems
                .filter { armsIds.contains(it.contractReferenceItemId) }
                .map { it.measuredQuantity }
                .fold(BigDecimal.ZERO) { acc, value -> acc + BigDecimal(value) }
                .toString()

        }

        if (streetItems.any { serviceId == it.contractReferenceItemId }) {
            if(listOf("0", "0.0").contains(autoQuantity) && autoCalculate) {
                streetItems = streetItems.filterNot { si -> serviceId == si.contractReferenceItemId }

                message = "Serviço de troca de ponto removido automaticamente"
            } else {
                streetItems = streetItems.map { si ->
                    if (serviceId == si.contractReferenceItemId) {
                        si.copy(measuredQuantity = autoQuantity)
                    } else si
                }

                if (autoCalculate) message = "Valor do serviço de troca de ponto definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
            }

        } else if (manuallyQuantity != "-1")  {
            streetItems = streetItems + PreMeasurementStreetItem(
                preMeasurementStreetId = preMeasurementStreetId.toString(),
                preMeasurementId = preMeasurementId.toString(),
                contractReferenceItemId = serviceId ?: 0,
                measuredQuantity = if (autoQuantity == "0") manuallyQuantity else autoQuantity
            )
            if(autoCalculate)
                message = "Valor do serviço de troca de ponto definido automaticamente para ${if (autoQuantity == "0") manuallyQuantity else autoQuantity}"
        }

    }

    fun removeItem(items: List<Item>, item: Item) {
        addUpdateOrRemoveItem(items, item, "-1")
    }

    fun setQuantity(items: List<Item>, item: Item, measuredQuantity: String) {
        streetItems = streetItems.map { si ->
            if (si.contractReferenceItemId == item.contractReferenceItemId) {
                si.copy(measuredQuantity = measuredQuantity)
            } else si
        }

        if(!listOf("0", "0.0").contains(measuredQuantity)) {
            when (item.type) {
                "LED" -> {
                    calculateRelay(items, null, "-1")
                    calculateLedService(items, null, "-1")
                    calculateProject(items, null, "-1")
                }

                "REFLETOR" -> {
                    calculateLedService(items, null, "-1")
                }

                "BRAÇO" -> {
                    calculateCable(items, null, "-1")
                    calculateArmService(items, null, "-1")
                }
            }
        }
    }

    fun save(coordinates: CoordinatesService) {
        viewModelScope.launch {
            loading = true
            try {
                val (lat, long) = coordinates.execute()
                if (lat != null && long != null) {
                    street = street?.copy(
                        latitude = lat,
                        longitude = long,
                    )
                }

                withContext(Dispatchers.IO) {
                    repository?.save(street!!, streetItems)
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
                streets = fetched!! // atualiza o estado com os dados obtidos
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
